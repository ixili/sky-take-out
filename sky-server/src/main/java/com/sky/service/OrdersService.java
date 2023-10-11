package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderSubmitVO;
import org.springframework.transaction.annotation.Transactional;


/**
 * 订单表(Orders)表服务接口
 *
 * @author lixi
 * @since 2023-10-12 00:41:44
 */
public interface OrdersService extends IService<Orders> {

    @Transactional
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
}

