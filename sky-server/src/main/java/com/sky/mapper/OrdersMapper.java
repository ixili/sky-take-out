package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;


/**
 * 订单表(Orders)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-12 00:41:44
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

}

