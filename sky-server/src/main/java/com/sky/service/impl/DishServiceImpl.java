package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.*;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xi
 * @create 2023/10/1- 22:36
 */
@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    DishMapper dishMapper;
    @Autowired
    DishFlavorMapper dishFlavorMapper;
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        // 1.新增一条菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        save(dish);
        Long dishId = dish.getId();
        // 2.新增多条口味
        List<DishFlavor> list = dishDTO.getFlavors();
        if(list != null && list.size()>0){
            List<DishFlavor> dishFlavorslist = list.stream().map(i -> {
                i.setDishId(dishId);
                return i;
            }).collect(Collectors.toList());

            Db.saveBatch(dishFlavorslist);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<Dish> page = Page.of(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());

        Page<Dish> p = lambdaQuery()
                .eq(dishPageQueryDTO.getCategoryId() != null, Dish::getCategoryId, dishPageQueryDTO.getCategoryId())
                .like(dishPageQueryDTO.getName() != null, Dish::getName, dishPageQueryDTO.getName())
                .eq(dishPageQueryDTO.getStatus() != null, Dish::getStatus, dishPageQueryDTO.getStatus())
                .page(page);


        List<Dish> dishList = p.getRecords();
        List<DishVO> dishVoList = new ArrayList<DishVO>();
        for(int i = 0;i<dishList.size();i++){
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dishList.get(i),dishVO);
            dishVoList.add(dishVO);
        }
        List<DishVO> res = dishVoList.stream().map(i -> {
            List<DishFlavor> list = Db.lambdaQuery(DishFlavor.class)
                    .eq(i.getId() != null, DishFlavor::getDishId, i.getId())
                    .list();
            // 设置菜品喜爱口味
            i.setFlavors(list);
            // 设置菜品分类名
            i.setCategoryName(Db.lambdaQuery(Category.class).eq(Category::getId,i.getCategoryId()).one().getName());
            return i;
        }).collect(Collectors.toList());

        PageResult pageResult = new PageResult();
        pageResult.setTotal(p.getTotal());
        pageResult.setRecords(res);
        return pageResult;
    }

    @Override
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        lambdaUpdate()
                .eq(dish.getId()!=null,Dish::getId,dish.getId())
                .update(dish);
    }

    @Override
    public DishVO query(Long id) {
        Dish dish = lambdaQuery().eq(Dish::getId, id).one();
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        return dishVO;
    }

    @Override
    public void deleteByIds(List<Long> ids) {

        for (Long id : ids){
            Dish dish = lambdaQuery().eq(Dish::getId,id).one();
            // 起售不能不能删除
            if(Objects.equals(dish.getStatus(), StatusConstant.ENABLE)){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            // 被套餐关联商品不能删除
            if(!Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getDishId, id).list().isEmpty()){
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }

        }
        // 删除商品
        removeByIds(ids);
        // 关联口味也需要删除
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DishFlavor::getDishId,ids);

        dishFlavorMapper.delete(wrapper);
    }

    @Override
    public void status(Integer status, Long id) {
        lambdaUpdate().eq(id!=null,Dish::getId,id)
                .update(Dish.builder().status(status).id(id).build());
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = lambdaQuery()
                .eq(dish.getId()!=null,Dish::getId,dish.getId())
                .list();
                //dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = Db
                    .lambdaQuery(DishFlavor.class)
                    .eq(d.getId()!=null,DishFlavor::getDishId,d.getId())
                    .list();
//                    dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
