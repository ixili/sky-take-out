package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrdersMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xi
 * @create 2023/10/17- 1:12
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<Orders> list = Db.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, 5)
                .between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX))
                .list();
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<BigDecimal> turnoverList = new ArrayList<>(dateList.size());
        for(LocalDate date : dateList){
            LocalDateTime timestart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime timeEnd = LocalDateTime.of(date, LocalTime.MAX);
            BigDecimal bigDecimal = new BigDecimal(0);
            for(Orders order : list){
                if (order.getOrderTime().compareTo(timestart) >= 0 && order.getOrderTime().compareTo(timeEnd) <= 0){
                    bigDecimal = bigDecimal.add(order.getAmount());
                }
            }
            turnoverList.add(bigDecimal);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {

        List<User> list = Db.lambdaQuery(User.class)
                .lt(User::getCreateTime,LocalDateTime.of(end,LocalTime.MAX))
                .list();
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> allUsers = new ArrayList<>(dateList.size());
        List<Integer> newUsers = new ArrayList<>(dateList.size());

        List<BigDecimal> turnoverList = new ArrayList<>(dateList.size());
        for(LocalDate date : dateList){
            LocalDateTime timestart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime timeEnd = LocalDateTime.of(date, LocalTime.MAX);
            int newCount = 0;
            int allCount = 0;
            for(User user : list){
                if (user.getCreateTime().compareTo(timestart) >= 0 && user.getCreateTime().compareTo(timeEnd) <= 0){
                    newCount++;
                }
                if (user.getCreateTime().compareTo(timeEnd) <= 0){
                    allCount++;
                }
            }
            allUsers.add(allCount);
            newUsers.add(newCount);

        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(allUsers,","))
                .newUserList(StringUtils.join(newUsers,","))
                .build();

    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<Orders> list = Db.lambdaQuery(Orders.class)
                .between(Orders::getOrderTime, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX))
                .list();
        List<Long> ids = list.stream().map(l -> {
            return l.getId();
        }).collect(Collectors.toList());
        List<OrderDetail> orderDetailList = Db.lambdaQuery(OrderDetail.class)
                .in(OrderDetail::getOrderId, ids)
                .list();

//        Db.lambdaQuery(OrderDetail.class)
//                .in(OrderDetail::getOrderId, ids)
//                .groupBy(OrderDetail::getDishId)
//                .list();


        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> allOrderCount = new ArrayList<>();
        List<Integer> validOrderCount = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime timestart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime timeEnd = LocalDateTime.of(date, LocalTime.MAX);
            int all = 0;
            int valid = 0;
            for(Orders order : list){
                if (order.getOrderTime().compareTo(timestart) >= 0 && order.getOrderTime().compareTo(timeEnd) <= 0){
                    if(order.getStatus().equals(5)){
                        valid++;
                    }
                    all++;
                }
            }
            allOrderCount.add(all);
            validOrderCount.add(valid);


        }
        int valid = 0;
        for(Orders order : list){
            if(order.getStatus().equals(5)){
                valid++;
            }
        }
        double completionRate = list.size()== 0 ? 0 : ( double)valid/(double) list.size();

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCompletionRate(completionRate)
                .orderCountList(StringUtils.join(allOrderCount,","))
                .validOrderCountList(StringUtils.join(validOrderCount,","))
                .totalOrderCount(list.size())
                .validOrderCount(valid)
                .build();

    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        LocalDateTime start = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = null;
        salesTop10 =  ordersMapper.getSalesTop10(start, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String namesJoin = StringUtils.join(names, ",");
        String numbersJoin = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(namesJoin)
                .numberList(numbersJoin)
                .build();

    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. 查询数据库获取30天营业数据
        LocalDateTime beginDate = LocalDateTime.of(LocalDate.now().minusDays(30),LocalTime.MIN);
        LocalDate bDate = LocalDate.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.of(LocalDate.now().minusDays(1),LocalTime.MAX);
        LocalDate eDate = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(beginDate, endDate);

        // 2.通过POI 将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            XSSFSheet sheet = excel.getSheet("Sheet1");
            XSSFRow row = sheet.getRow(1);
            row.getCell(1).setCellValue("日期："+bDate + "至" + eDate);
            row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            // 填写 明细数据
            LocalDate date = LocalDate.now().minusDays(30);
            for(int i = 0;i < 30; i++){
                LocalDateTime start = LocalDateTime.of(date.plusDays(i),LocalTime.MIN);
                LocalDateTime end = LocalDateTime.of(date.plusDays(i),LocalTime.MAX);
                BusinessDataVO nowBusinessData = workspaceService.getBusinessData(start, end);
                row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.plusDays(i).toString());
                row.getCell(2).setCellValue(nowBusinessData.getTurnover());
                row.getCell(3).setCellValue(nowBusinessData.getValidOrderCount());
                row.getCell(4).setCellValue(nowBusinessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(nowBusinessData.getUnitPrice());
                row.getCell(6).setCellValue(nowBusinessData.getNewUsers());
            }


            // 3.下载excel到输出流
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            excel.close();
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
