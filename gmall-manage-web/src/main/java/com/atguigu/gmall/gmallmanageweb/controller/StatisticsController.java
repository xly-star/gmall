package com.atguigu.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SaleStatistics;
import com.atguigu.gmall.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author xulingyun
 * @create 2021-12-12 16:46
 */
@RestController
@CrossOrigin
@RequestMapping("statistics")
public class StatisticsController {

    @Reference
    private StatisticsService statisticsService;

    @PostMapping("/eCharts")
    public Map<String,Object> eCharts(@RequestBody SaleStatistics saleStatistics){
        System.out.println("*********"+saleStatistics);
        return statisticsService.getEChartsByDate(saleStatistics);
    }

}
