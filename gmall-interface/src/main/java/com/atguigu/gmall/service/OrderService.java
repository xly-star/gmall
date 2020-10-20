package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

/**
 * @author xulingyun
 * @create 2020-10-16 10:50
 */
public interface OrderService {

    /**
     * 保存订单信息到订单表和订单详情表
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);


    /**
     * 在访问订单页面时生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 在提交订单时检验流水号是否和redis中的相等，相等则跳转生成订单，不相等则说明订单重复提交跳转失败页面
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 在生成订单后删除redis中的流水号
     * @param userId
     */
    void  delTradeCode(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据orderId查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 支付完成消费者调用该方法修改订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 生产者异步调用仓库，通知仓库减库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 传入orderId制作参数
     * @param orderInfo
     * @return
     */
    Map<String ,Object> initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单接口
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
