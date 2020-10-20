package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-09-28 20:15
 */
public interface UserService {
    /**
     *
     * @return返回所有用户信息
     */
     List<UserInfo> getAllUserInfo();

    /**
     *
     * @param userId
     * @return 方法用户地址
     */
     List<UserAddress> getUserAddressById(String userId);


    /**
     * 验证登录
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据传入的userId验证用户是否登录
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
