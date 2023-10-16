package com.sky.service;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.Orders;
import com.sky.service.impl.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xi
 * @create 2023/10/17- 1:12
 */
@Service
public class ReportServiceImpl implements ReportService {
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<Orders> list = Db.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, 5)
                .between(Orders::getOrderTime, begin, end)
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
}
