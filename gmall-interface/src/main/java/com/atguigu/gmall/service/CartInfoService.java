package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-14 15:03
 */
public interface CartInfoService {
    /**
     * 加入购物车登录状态
     * @return
     * @param skuId
     * @param userId
     * @param num
     */
    public void   addToCart(String skuId, String userId, Integer num);

    /**
     * 根据userId查询用户购物车详情
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId);

    /**
     * 改变选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据userId查询redis中的被选中的商品
     * @param userId
     * @return
     */
    List<OrderDetail> getCartCheckedList(String userId);


    /**
     *
     * 删除生成订单的后购物车信息
     * @param userId
     */
    void delCartInfo(String userId);

    /**
     * 更新缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadInfoToRedis(String userId);

}
