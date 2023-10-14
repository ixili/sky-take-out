package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


/**
 * 订单表(Orders)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-12 00:41:44
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
//    void update(Orders orders);
}

