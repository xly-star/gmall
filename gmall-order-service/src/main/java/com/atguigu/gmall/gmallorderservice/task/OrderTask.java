package com.atguigu.gmall.gmallorderservice.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-20 18:25
 */
//@Component
//@EnableScheduling
public class OrderTask {

    @Reference
    private OrderService orderService;

    // 5 每分钟的第五秒
    // 0/5 没隔五秒执行一次
    @Scheduled(cron = "5 * * * * ?")
    public void work() {
        System.out.println("-------------001----------");
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void work2() {
        System.out.println("-------------002----------");
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder() {
        System.out.println("开始处理过期订单");
        long start = System.currentTimeMillis();
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : expiredOrderList) {
            //处理过期订单
            orderService.execExpiredOrder(orderInfo);
        }
        long end = System.currentTimeMillis();
        System.out.println("一共处理" + expiredOrderList.size() + "个订单 共消耗" + (end - start) + "毫秒");

    }
}
