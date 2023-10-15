package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.service.ShopService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单表(Orders)表服务实现类
 *
 * @author lixi
 * @since 2023-10-12 00:41:44
 */
@Service("ordersService")
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

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


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
        User user = Db.lambdaQuery(User.class).eq(User::getId,userId).one();

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        //生成空Json 跳过支付
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
//        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        Orders ordersDB = lambdaQuery().eq(outTradeNo!=null,Orders::getNumber,outTradeNo).one();

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

//        orderMapper.update(orders);
        lambdaUpdate().eq(orders.getId()!=null,Orders::getId,orders.getId()).update(orders);

        // 通过websocket 推送给店家 来单了
        Map map = new HashMap();
        map.put("type",1);// 1来单提醒 2客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号"+outTradeNo);

        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult pageQueryforUser(int page, int pageSize, Integer status) {
        Page<Orders> p = Page.of(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> ordersPage = lambdaQuery()
                .eq(ordersPageQueryDTO.getNumber()!=null,Orders::getNumber, ordersPageQueryDTO.getNumber())
                .eq(ordersPageQueryDTO.getPhone()!=null,Orders::getPhone, ordersPageQueryDTO.getPhone())
                .gt(ordersPageQueryDTO.getBeginTime()!=null,Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .lt(ordersPageQueryDTO.getEndTime()!=null,Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .eq(BaseContext.getCurrentId() != null, Orders::getUserId, BaseContext.getCurrentId())
                .eq(status != null, Orders::getStatus, status)
                .orderByDesc(Orders::getOrderTime)
                .page(p);

//        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (ordersPage != null && ordersPage.getTotal() > 0) {
            for (Orders orders : ordersPage.getRecords()) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
//                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId,orderId).list();

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(ordersPage.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = lambdaQuery().eq(Orders::getId,id).one();

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId,orders.getId()).list();

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = lambdaQuery().eq(Orders::getId,id).one();

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId,id).list();

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        Db.saveBatch(shoppingCartList);
//        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
//        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = Page.of(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> p = lambdaQuery()
                .eq(ordersPageQueryDTO.getNumber()!=null,Orders::getNumber, ordersPageQueryDTO.getNumber())
                .eq(ordersPageQueryDTO.getPhone()!=null,Orders::getPhone, ordersPageQueryDTO.getPhone())
                .eq(ordersPageQueryDTO.getUserId()!=null,Orders::getUserId, ordersPageQueryDTO.getUserId())
                .eq(ordersPageQueryDTO.getStatus()!=null,Orders::getStatus, ordersPageQueryDTO.getStatus())
                .gt(ordersPageQueryDTO.getBeginTime()!=null,Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .lt(ordersPageQueryDTO.getEndTime()!=null,Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .orderByDesc(Orders::getOrderTime)
                .page(page);

//        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(p);

        return new PageResult(page.getTotal(), orderVOList);
    }



    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getRecords();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId,orders.getId()).list();

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
//        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
//        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
//        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = Math.toIntExact(lambdaQuery().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED).count());
        Integer confirmed = Math.toIntExact(lambdaQuery().eq(Orders::getStatus, Orders.CONFIRMED).count());
        Integer deliveryInProgress = Math.toIntExact(lambdaQuery().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS).count());

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);

    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 根据id查询订单
        Orders ordersDB = lambdaQuery().eq(Orders::getId,ordersRejectionDTO.getId()).one();

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        Orders ordersDB = lambdaQuery().eq(Orders::getId,ordersCancelDTO.getId()).one();

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = lambdaQuery().eq(Orders::getId,id).one();

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = lambdaQuery().eq(Orders::getId,id).one();

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        lambdaUpdate().eq(Orders::getId,orders.getId()).update(orders);
    }

    @Override
    public void reminder(Long id) {
        // 通过websocket 推送给店家 来单了

        Map map = new HashMap();
        map.put("type",2);// 1来单提醒 2客户催单
        map.put("orderId",id);
        Orders one = lambdaQuery().eq(Orders::getId, id).one();
        if(one == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        map.put("content","订单号"+one.getNumber());

        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);

    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}

