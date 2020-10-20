package com.atguigu.gmall.gmallcartweb.handler;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-14 15:30
 */
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE = 7 * 24 * 3600;
    @Reference
    private ManageService manageService;


    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer num) {
        //查看cookie中是否有该商品有就修改数量，实时价格，
        //没有就添加
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        boolean ifExist = false;
        List<CartInfo> cartInfos = new ArrayList<>();
        if (StringUtils.isNotEmpty(cookieValue)) {
            cartInfos = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfos) {
                if (cartInfo.getSkuId().equals(skuId)) {
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + num);
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist = true;
                    break;
                }
            }
        }
        //如果购物车中没有相同的商品就加入
        if (!ifExist) {
            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(num);
            cartInfos.add(cartInfo);
        }
        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartInfos), COOKIE_CART_MAXAGE, true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (StringUtils.isNotEmpty(cookieValue)) {
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            return cartInfoList;
        }
        return null;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (cookieValue != null) {
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            if (cartInfoList != null && cartInfoList.size() > 0) {
                for (CartInfo cartInfo : cartInfoList) {
                    if (skuId.equals(cartInfo.getSkuId())) {
                        cartInfo.setIsChecked(isChecked);
                        break;
                    }
                }
            }
            CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartInfoList), COOKIE_CART_MAXAGE, true);
        }
    }
}
