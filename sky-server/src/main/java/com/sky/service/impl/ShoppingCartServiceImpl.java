package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车(ShoppingCart)表服务实现类
 *
 * @author lixi
 * @since 2023-10-08 21:09:04
 */
@Service("shoppingCartService")
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        Long currentId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCart.setUserId(currentId);
        // 如果是菜品
        if(shoppingCartDTO.getDishId()!=null){
            Dish dish = Db.lambdaQuery(Dish.class).eq(Dish::getId,shoppingCartDTO.getDishId()).one();
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setName(dish.getName());
            shoppingCart.setAmount(dish.getPrice());
            // 如果原来有 菜品
            ShoppingCart one = lambdaQuery()
                    .eq(ShoppingCart::getUserId, currentId)
                    .eq(ShoppingCart::getDishId,shoppingCartDTO.getDishId())
                    .eq(shoppingCartDTO.getDishFlavor()!=null,ShoppingCart::getDishFlavor,shoppingCartDTO.getDishFlavor())
                    .one();
            if(one!=null){
                shoppingCart.setNumber(one.getNumber()+1);
                LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
                wrapper.eq(ShoppingCart::getUserId,currentId)
                        .eq(ShoppingCart::getDishId,shoppingCartDTO.getDishId())
                        .eq(shoppingCartDTO.getDishFlavor()!=null,ShoppingCart::getDishFlavor,shoppingCartDTO.getDishFlavor());

                update(shoppingCart,wrapper);
            }else{
                shoppingCart.setNumber(1);
                save(shoppingCart);
            }
        }
        // 如果是套餐
        else{
            Setmeal setmeal = Db.lambdaQuery(Setmeal.class).eq(Setmeal::getId,shoppingCartDTO.getSetmealId()).one();
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setAmount(setmeal.getPrice());
            // 如果原来有 套餐
            ShoppingCart one = lambdaQuery()
                    .eq(ShoppingCart::getUserId, currentId)
                    .eq(ShoppingCart::getSetmealId,shoppingCartDTO.getSetmealId())
                    .one();
            if(one!=null){
                shoppingCart.setNumber(one.getNumber()+1);
                LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
                wrapper.eq(ShoppingCart::getUserId,currentId)
                        .eq(ShoppingCart::getSetmealId,shoppingCartDTO.getSetmealId());
                update(shoppingCart,wrapper);
            }else{
                shoppingCart.setNumber(1);
                save(shoppingCart);
            }
        }
//        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
//        wrapper.eq(ShoppingCart::getUserId,currentId);
//        save(shoppingCart);

    }
}

