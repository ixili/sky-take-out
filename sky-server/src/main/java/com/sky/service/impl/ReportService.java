package com.sky.service.impl;

import com.sky.vo.TurnoverReportVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * @author xi
 * @create 2023/10/17- 1:11
 */

public interface ReportService {
    TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);
}
