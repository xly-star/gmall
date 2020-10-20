package com.atguigu.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author xulingyun
 * @create 2020-10-20 15:48
 */
@Component
public class PaymentConsumer {
    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        boolean flag = paymentService.checkPayment(paymentInfo);
        System.out.println("检查结果：" + flag);
        if (!flag && checkCount > 0) {
            // 还需要继续检查
            System.out.println("检查的次数：" + checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo, delaySec, checkCount - 1);
        }
    }
}
