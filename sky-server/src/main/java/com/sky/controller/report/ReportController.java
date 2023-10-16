package com.sky.controller.report;

import com.sky.result.Result;
import com.sky.service.impl.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * @author xi
 * @create 2023/10/17- 1:06
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "数据统计接口")
public class ReportController {
    @Autowired
    ReportService reportService;

    @ApiOperation("营业额统计接口")
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        log.info("营业额统计,{},{}",begin,end);
        TurnoverReportVO turnoverReportVO = reportService.turnoverStatistics(begin,end);
        return Result.success(turnoverReportVO);
    }

    @ApiOperation("用户统计接口")
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        log.info("用户统计接口,{},{}",begin,end);
        UserReportVO userReportVO = reportService.userStatistics(begin,end);
        return Result.success(userReportVO);
    }


    @ApiOperation("订单统计接口")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        log.info("订单统计接口,{},{}",begin,end);
        OrderReportVO orderReportVO = reportService.ordersStatistics(begin,end);
        return Result.success(orderReportVO);
    }
}
