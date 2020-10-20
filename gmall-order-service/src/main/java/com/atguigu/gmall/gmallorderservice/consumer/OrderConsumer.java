package com.atguigu.gmall.gmallorderservice.consumer;

import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author xulingyun
 * @create 2020-10-19 19:10
 */
@Component
public class OrderConsumer {
    @Autowired
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        //说明支付成功
        if ("success".equals(result)){
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            //通知库存减库存
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }else{
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if ("DEDUCTED".equals(status)){
            //减库存成功
            orderService.updateOrderStatus(orderId, ProcessStatus.WAITING_DELEVER);
        }else{
            //减库存失败，库存超卖
            orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
        }

    }
}
