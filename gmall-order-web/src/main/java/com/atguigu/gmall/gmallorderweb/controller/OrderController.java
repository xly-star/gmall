package com.atguigu.gmall.gmallorderweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author xulingyun
 * @create 2020-09-28 22:26
 */
@Controller
@Slf4j
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartInfoService cartInfoService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId != null && userId.length() > 0) {
            //渲染地址
            List<UserAddress> addressList = userService.getUserAddressById(userId);
            request.setAttribute("addressList", addressList);

            //渲染订单详情
            List<OrderDetail> orderDetailList = cartInfoService.getCartCheckedList(userId);
            request.setAttribute("orderDetailList", orderDetailList);

            if (orderDetailList == null || orderDetailList.size() == 0) {
                request.setAttribute("errMsg", "未选中商品!");
                return "tradeFail";
            }

            //渲染总价格
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderDetailList(orderDetailList);
            orderInfo.sumTotalAmount();
            request.setAttribute("totalAmount", orderInfo.getTotalAmount());

            //生成页面流水号
            String tradeNo = orderService.getTradeNo(userId);
            request.setAttribute("tradeNo", tradeNo);

        }
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo) {
        System.out.println(JSONObject.toJSONString(orderInfo));
        String userId = (String) request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        //检验tradeNo
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            request.setAttribute("errMsg", "表单不能重复提交!");
            return "tradeFail";
        }

        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean res = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!res) {
                request.setAttribute("errMsg", "库存不足!");
                return "tradeFail";
            }

            //验证价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int result = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (result != 0) {
                request.setAttribute("errMsg", "商品价格发生变化,请重新下单!");
                cartInfoService.loadInfoToRedis(userId);
                //删除redis中被选中的集合，因为mysql和redis的购物车中isChecked都是0所以不会被删除
                cartInfoService.delCartInfo(userId);
                return "tradeFail";
            }
        }


        //初始化参数
        orderInfo.setUserId(userId);
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //保存订单
        String orderId = orderService.saveOrder(orderInfo);

        //删除流水号
        orderService.delTradeCode(userId);
        //删除redis和mysql中的购物车信息
        cartInfoService.delCartInfo(userId);
        return "redirect://payment.gmall.com/index?orderId=" + orderId;

    }


    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //查询出子订单
        List<OrderInfo> orderInfoList = orderService.splitOrder(orderId, wareSkuMap);

        ArrayList<Map> maps = new ArrayList<>();
        for (OrderInfo orderInfo : orderInfoList) {
            Map<String, Object> map = orderService.initWareOrder(orderInfo);
            maps.add(map);
        }

        return JSON.toJSONString(maps);
    }

    @RequestMapping("toOrder")
    @LoginRequire(autoRedirect = true)
    public String toOrder(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        UserInfo userName = userService.getUserNameByUserId(userId);
        log.info("userId = {}", userId);
        System.out.println("******用户****"+userId);
        List<OrderInfo> orderListByUserId = orderService.getOrderListByUserId(userId);
        request.setAttribute("orderList", orderListByUserId);
        request.setAttribute("userInfo",userName);
        System.out.println(JSONObject.toJSONString(orderListByUserId));
        return "list";
    }

}
