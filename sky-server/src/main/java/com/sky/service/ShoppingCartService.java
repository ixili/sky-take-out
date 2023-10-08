package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;


/**
 * 购物车(ShoppingCart)表服务接口
 *
 * @author lixi
 * @since 2023-10-08 21:09:04
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    void add(ShoppingCartDTO shoppingCartDTO);
}

