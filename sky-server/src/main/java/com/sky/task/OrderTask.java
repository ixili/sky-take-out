package com.sky.task;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author xi
 * @create 2023/10/15- 22:34
 */
@Component
@Slf4j
public class OrderTask {
    @Scheduled( cron = "0 * * * * ? ")
    public void processTimeoutOrder(){
        LocalDateTime currentTime = LocalDateTime.now();
        log.info("处理未支付订单：{}", currentTime);
        LocalDateTime ltTime = currentTime.minusMinutes(15);
        List<Orders> list = Db.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .le(Orders::getOrderTime,ltTime)
                .list();
        if(list!=null && list.size()>0){

            for(Orders order : list){
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(currentTime);
                order.setCancelReason("订单支付超时");
                Db.lambdaUpdate(Orders.class)
                        .eq(Orders::getId,order.getId())
                        .update(order);
            }
        }
    }

    @Scheduled( cron = "0 0 1 * * ? ")
    public void processDeliveryTimeoutOrder(){
        LocalDateTime currentTime = LocalDateTime.now();
        log.info("处理未配送订单：{}", currentTime);
        LocalDateTime ltTime = currentTime.minusMinutes(60);
        List<Orders> list = Db.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .le(Orders::getOrderTime,ltTime)
                .list();
        if(list!=null && list.size()>0){

            for(Orders order : list){
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(currentTime);
                order.setCancelReason("订单未配送");
                Db.lambdaUpdate(Orders.class)
                        .eq(Orders::getId,order.getId())
                        .update(order);
            }
        }
    }
}
