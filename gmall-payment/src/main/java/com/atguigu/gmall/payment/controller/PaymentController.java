package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author xulingyun
 * @create 2020-10-17 19:22
 */
@Controller
public class PaymentController {
    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request) {
        // 根据userId查询用户 写LoginRequire注解
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("orderId", orderId);
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        return "index";
    }


    @RequestMapping("/alipay/submit")
    @ResponseBody
    @LoginRequire
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response) {
        //TODO 将支付宝或者微信支付成功的结果写入数据库，当调用支付宝或微信时判断商品是否已经支付

        // 获取订单Id
        String orderId = request.getParameter("orderId");
        // 取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("买手机");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());

        // 保存信息
        paymentService.savePaymentInfo(paymentInfo);

        // 支付宝参数

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        // 声明一个Map
        Map<String, Object> bizContnetMap = new HashMap<>();
        bizContnetMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject", paymentInfo.getSubject());
        bizContnetMap.put("total_amount", paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        // 代码追后面 15秒执行一次，总共需要执行3次。
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    @RequestMapping("alipay/callback/return")
    @LoginRequire
    public String alipayCallback() {
        return "redirect:" + AlipayConfig.return_order_url;
    }

    @RequestMapping("/alipay/callback/notify")
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) throws AlipayApiException {
        boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        if (!flag) {
            return "fail";
        }
        //判断结束
        String trade_status = paramMap.get("trade_status");
        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
            // 查单据是否处理
            String out_trade_no = paramMap.get("out_trade_no");
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOutTradeNo(out_trade_no);
            PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo);
            if (paymentInfoHas.getPaymentStatus() == PaymentStatus.PAID || paymentInfoHas.getPaymentStatus() == PaymentStatus.ClOSED) {
                return "fail";
            } else {
                // 修改
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // 设置状态
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfoUpd.setCallbackTime(new Date());
                // 设置内容
                paymentInfoUpd.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(out_trade_no, paymentInfoUpd);
                //支付成功回调成功发送消息给订单模块修改状态
                sendPaymentResult(paymentInfo,"success");
                return "success";
            }
        }
        return "fail";
    }

    @RequestMapping("sendPaymentResult")
    @ResponseBody
    private String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result")String result) {
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }

    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId) {
        boolean flag = paymentService.refund(orderId);
        System.out.println("flag:" + flag);
        //修改订单状态
        if (flag) {
            // 修改
            PaymentInfo paymentInfoUpd = new PaymentInfo();
            // 设置状态
            paymentInfoUpd.setPaymentStatus(PaymentStatus.REFUND);
            paymentService.updatePaymentInfo(paymentInfoUpd, orderId);
        }
        return flag + "";
    }

    @RequestMapping("wx/submit")
    @ResponseBody
    @LoginRequire
    public Map<String, String> createNative(String orderId) {
        // 做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！
        // 调用服务层数据
        // 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        orderId = UUID.randomUUID().toString().replaceAll("-", "");
        Map map = paymentService.createNative(orderId + "", "1");
        System.out.println("code_url = " + map.get("code_url"));
        // data = map
        return map;
    }

    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return ""+flag;

    }
}
