package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;


/**
 * 购物车(ShoppingCart)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-08 21:09:04
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

}

