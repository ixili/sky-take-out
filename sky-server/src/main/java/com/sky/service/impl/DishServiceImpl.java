package com.sky.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
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
}
