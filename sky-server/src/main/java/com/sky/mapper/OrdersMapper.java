package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


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

//    /**
//     * 修改订单信息
//     * @param orders
//     */
//    void update(Orders orders);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    /**
     * 根据动态条件统计营业额
     * @param map
     */
    Double sumByMap(Map map);

    /**
     *根据动态条件统计订单数量
     * @param map
     */
    Integer countByMap(Map map);
}

