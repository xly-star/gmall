package com.atguigu.gmall.gmallcartweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.gmallcartweb.handler.CartCookieHandler;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-14 14:47
 */
@Controller
public class CartController {

    @Reference
    private CartInfoService cartInfoService;

    @Reference
    private ManageService manageService;


    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(String skuId, Integer num, HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        //登录 加入数据库和redis
        if (StringUtils.isNotEmpty(userId)) {
            cartInfoService.addToCart(skuId, userId, num);
        } else {
            //未登录 加入cookie中
            cartCookieHandler.addToCart(request, response, skuId, userId, num);
        }

        // 取得sku信息对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", num);

        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isNotEmpty(userId)) {
            //登录状态 下查看购物车,如果cookie中有购物车信息则合并，没有就从redis中取得数据
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            if (cartList != null && cartList.size() > 0) {
                // 开始合并
                cartInfoList = cartInfoService.mergeToCartList(cartList, userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                //cookie中没有购物车信息
                cartInfoList = cartInfoService.getCartList(userId);
            }
        } else {
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList", cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        if (StringUtils.isNotEmpty(userId)) {
            //登录状态下改变check状态
            cartInfoService.checkCart(skuId, isChecked, userId);
        } else {
            //未登录
            cartCookieHandler.checkCart(request, response, skuId, isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String  userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList = cartCookieHandler.getCartList(request);
        if (cartList != null && cartList.size() > 0) {
            //合并购物车
            cartInfoService.mergeToCartList(cartList, userId);
            //删除cookie
            cartCookieHandler.deleteCartCookie(request, response);
        }
        return "redirect:http://order.gmall.com/trade";
    }
}
