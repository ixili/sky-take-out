package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;


/**
 * 订单明细表(OrderDetail)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-12 00:42:00
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}

