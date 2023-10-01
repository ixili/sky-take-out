package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

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
}
