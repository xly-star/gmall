package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SaleStatistics;

import java.util.Map;

/**
 * @author xulingyun
 * @create 2021-12-12 16:48
 */
public interface StatisticsService {

    /**
     *根据三级分类id获取订单支付成功量
     * @param SaleStatistics
     * @return
     */
    public Map<String, Object> getEChartsByDate(SaleStatistics SaleStatistics);

}
