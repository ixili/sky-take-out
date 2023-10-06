package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** 菜品相关接口
 * @author xi
 * @create 2023/10/1- 22:46
 */
@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        deleteCache("dish_" + dishDTO.getCategoryId());
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询：{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.update(dishDTO);

        deleteCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据Id获取菜品")
    public Result<DishVO> query(@PathVariable("id") Long id){
        log.info("根据Id获取菜品：{}",id);
        DishVO dishVO = dishService.query(id);
        return Result.success(dishVO);
    }

    @DeleteMapping
    @ApiOperation("根据Ids删除菜品")
    // 默认为数组Long[] 加注解可以转为集合
    public Result delete(@RequestParam List<Long> ids){
        log.info("根据Ids删除菜品：{}",ids);
        dishService.deleteByIds(ids);

        deleteCache("dish_*");
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品起售停售状态")
    // 默认为数组Long[] 加注解可以转为集合
    public Result status(@PathVariable Integer status,Long id){
        log.info("修改菜品起售停售状态：{},{}",status,id);
        dishService.status(status,id);

        deleteCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    // 默认为数组Long[] 加注解可以转为集合
    public Result<List<Dish>> status(Long categoryId){
        log.info("根据分类id查询菜品：{}",categoryId);
        List<Dish> list = dishService.lambdaQuery().eq(Dish::getCategoryId, categoryId).list();
        return Result.success(list);
    }

    private void deleteCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
