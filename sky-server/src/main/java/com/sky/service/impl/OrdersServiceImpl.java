package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.service.OrdersService;
import com.sky.service.ShopService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单表(Orders)表服务实现类
 *
 * @author lixi
 * @since 2023-10-12 00:41:44
 */
@Service("ordersService")
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Override
    public OrderSubmitVO submit( OrdersSubmitDTO ordersSubmitDTO) {
        // 1.处理异常业务异常（地址为空，购物车为空）
        AddressBook addressBook = Db.lambdaQuery(AddressBook.class).eq(AddressBook::getId, ordersSubmitDTO.getAddressBookId()).one();
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        List<ShoppingCart> list = Db.lambdaQuery(ShoppingCart.class).eq(ShoppingCart::getUserId, BaseContext.getCurrentId()).list();
        if(list == null || list.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2.向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setAddress(addressBook.getDetail());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayMethod(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        // 订单号取时间戳
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());
//        orders.setUserName(addressBook.getConsignee());
        save(orders);

        // 3.向订单细节表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : list){
            // 订单明细
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            // 设置当前订单明细关联的订单ID
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        Db.saveBatch(orderDetailList);

        // 4.删除购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseContext.getCurrentId()!=null,ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(wrapper);



        // 5.返回VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO
                .builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }
}

