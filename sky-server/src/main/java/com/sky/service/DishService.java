package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author xi
 * @create 2023/10/1- 22:34
 */
public interface DishService extends IService<Dish> {
    @Transactional
    void saveWithFlavor(DishDTO dishDTO);
}
