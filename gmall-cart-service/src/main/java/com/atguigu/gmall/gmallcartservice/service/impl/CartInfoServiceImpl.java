package com.atguigu.gmall.gmallcartservice.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.gmallcartservice.mapper.CartInfoMapper;
import com.atguigu.gmall.gmallcartservice.util.CartConst;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-14 15:06
 */
@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Reference
    private ManageService manageService;

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer num) {

        CartInfo cartInfoSelect = new CartInfo();
        cartInfoSelect.setUserId(userId);
        cartInfoSelect.setSkuId(skuId);
        CartInfo cartInfo = cartInfoMapper.selectOne(cartInfoSelect);
        //查询商品信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if (skuId != null) {
            //当前添加的商品在购物车中已经存在数量修改，
            if (cartInfo != null) {
                cartInfo.setSkuNum(cartInfo.getSkuNum() + num);
                cartInfo.setSkuPrice(skuInfo.getPrice());

                cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
            } else {
                //当前添加商品购物车没有直接加入购物车
                cartInfo = new CartInfo();

                cartInfo.setSkuId(skuId);
                cartInfo.setCartPrice(skuInfo.getPrice());
                cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
                cartInfo.setSkuName(skuInfo.getSkuName());
                cartInfo.setSkuNum(num);
                cartInfo.setSkuPrice(skuInfo.getPrice());
                cartInfo.setUserId(userId);

                cartInfoMapper.insertSelective(cartInfo);
            }
            //将添加的商品加入缓存
            Jedis jedis = redisUtil.getJedis();
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfo));
            jedis.close();
        }
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //如果redis中有数据从redis中读取数据
        //redis没有数据则从数据库中读取数据在加载到redis中
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartKey);
        List<CartInfo> cartList = new ArrayList<>();
        if (stringList != null && stringList.size() > 0) {
            for (String string : stringList) {
                CartInfo cartInfo = JSON.parseObject(string, CartInfo.class);
                cartList.add(cartInfo);
            }
            cartList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()), Long.parseLong(o1.getId()));
                }
            });
        } else {
            cartList = loadInfoToRedis(userId, jedis);
        }
        if (jedis != null) {
            jedis.close();
        }
        return cartList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        for (CartInfo cartCookieInfo : cartList) {
            boolean isMatch = false;
            for (CartInfo cartDataBaseInfo : cartInfoList) {
                if (cartCookieInfo.getSkuId().equals(cartDataBaseInfo.getSkuId())) {
                    cartDataBaseInfo.setSkuNum(cartDataBaseInfo.getSkuNum() + cartCookieInfo.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartDataBaseInfo);
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                cartCookieInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartCookieInfo);
            }
        }
        Jedis jedis = redisUtil.getJedis();
        cartInfoList = loadInfoToRedis(userId, jedis);
        //删除选中被选中的redis key不然当登录有勾选商品然后在未登录状态选中商品在登录时会出现错误
        String cartCheckKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> cartInfoListJson = jedis.hvals(cartCheckKey);
        if (cartInfoListJson != null && cartInfoListJson.size() > 0) {
            for (String cartInfoJson : cartInfoListJson) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                jedis.hdel(cartCheckKey, cartInfo.getSkuId());
            }
        }

        //修改选中状态已cookie为基准
        for (CartInfo cartDBInfo : cartInfoList) {
            for (CartInfo cartCKInfo : cartList) {
                if (cartCKInfo.getSkuId().equals(cartDBInfo.getSkuId())) {
                    if ("1".equals(cartCKInfo.getIsChecked())) {
                        //修改选中状态已cookie为基准
                        cartDBInfo.setIsChecked(cartCKInfo.getIsChecked());
                        checkCart(cartDBInfo.getSkuId(), cartDBInfo.getIsChecked(), userId);
                    }
                    break;
                }
            }
        }
        if (jedis != null) {
            jedis.close();
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /*
        1.获取jedis
        2.得到redis数据
        3.根据传入的check修改redis中的数据
        4.写回redis
        5.添加一个新的redis数据保存需要结算的数据
         */
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartInfoJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartCheckdJson = JSON.toJSONString(cartInfo);
        jedis.hset(cartKey, skuId, cartCheckdJson);

        String cartCheckKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)) {
            jedis.hset(cartCheckKey, skuId, cartCheckdJson);
        } else {
            jedis.hdel(cartCheckKey, skuId);
        }
        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public List<OrderDetail> getCartCheckedList(String userId) {
        //1.获取redis
        Jedis jedis = redisUtil.getJedis();
        ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
        //2.从redis中获取数据
        String cartCheckKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> cartInfoJson = jedis.hvals(cartCheckKey);
        if (cartInfoJson != null && cartInfoJson.size() > 0) {
            for (String cartInfo : cartInfoJson) {
                CartInfo info = JSON.parseObject(cartInfo, CartInfo.class);
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(info.getSkuId());
                orderDetail.setSkuName(info.getSkuName());
                orderDetail.setImgUrl(info.getImgUrl());
                orderDetail.setSkuNum(info.getSkuNum());
                orderDetail.setOrderPrice(info.getCartPrice());

                orderDetailArrayList.add(orderDetail);
            }
        }
        //3.返回结果
        return orderDetailArrayList;
    }

    @Override
    public void delCartInfo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //删除redis中要购买的商品列表
        String cartCheckKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> cartInfoListJson = jedis.hvals(cartCheckKey);
        if (cartInfoListJson != null && cartInfoListJson.size() > 0) {
            for (String cartInfoJson : cartInfoListJson) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                jedis.hdel(cartCheckKey, cartInfo.getSkuId());
            }
        }
        //删除购物车中被选中的商品
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartStringList = jedis.hvals(cartKey);
        if (cartStringList != null && cartStringList.size() > 0) {
            CartInfo info = new CartInfo();
            for (String cartInfoJson : cartStringList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                if ("1".equals(cartInfo.getIsChecked())) {
                    jedis.hdel(cartKey, cartInfo.getSkuId());

                    info.setUserId(cartInfo.getUserId());
                    info.setSkuId(cartInfo.getSkuId());
                    cartInfoMapper.delete(info);
                }
            }
        }
    }

    @Override
    public List<CartInfo> loadInfoToRedis(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //实时查询价格
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            for (CartInfo info : cartInfoList) {
                jedis.hset(cartKey, info.getSkuId(), JSON.toJSONString(info));
            }
        }
        jedis.close();
        return cartInfoList;
    }

    public List<CartInfo> loadInfoToRedis(String userId, Jedis jedis) {
        //实时查询价格
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            for (CartInfo info : cartInfoList) {
                jedis.hset(cartKey, info.getSkuId(), JSON.toJSONString(info));
            }
        }
        return cartInfoList;
    }
}
