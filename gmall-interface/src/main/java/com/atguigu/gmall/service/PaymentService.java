package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author xulingyun
 * @create 2020-10-17 19:43
 */
public interface PaymentService {
    /**
     *保存支付订单信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 异步回调返回支付信息
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 异步回调成功修改支付状态
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 修改订单状态为退款
     * @param paymentInfoUpd
     * @param orderId
     */
    void updatePaymentInfo(PaymentInfo paymentInfoUpd, String orderId);

    /**
     * 微信支付生成二维码
     * @param s
     * @param s1
     * @return
     */
    Map createNative(String s, String s1);

    /**
     * 发送activemq给订单模块
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    /**
     * 根据orderId查询第三方交易号在与支付宝交互查询该订单是否支付
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 在生成支付宝二维码时主动去询问支付宝该订单是否支付解决消息队列的不确定性，延迟队列
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);
}
