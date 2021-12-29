package com.atguigu.gmall.gmallusermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.gmallusermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.gmallusermanage.mapper.UserMapper;
import com.atguigu.gmall.service.UserService;
import io.searchbox.client.JestClient;
import org.apache.tomcat.util.digester.Digester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author xulingyun
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix = "user:";
    public String userinfoKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60 * 24;

    @Override
    public List<UserInfo> getAllUserInfo() {
        return userMapper.selectAll();
    }

    @Override
    public UserInfo getUserNameByUserId(String userId) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        return userMapper.selectOne(userInfo);
    }

    @Override
    public List<UserAddress> getUserAddressById(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String digestAsHex = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(digestAsHex);
        UserInfo info = userMapper.selectOne(userInfo);

        if (info != null) {
            String key = userKey_prefix + info.getId() + userinfoKey_suffix;
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(key, userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
        }
        return info;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String key = userKey_prefix + userId + userinfoKey_suffix;
            String userJson = jedis.get(key);
            // 延长时效
            jedis.expire(key, userKey_timeOut);
            if (userJson != null) {
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return userInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public String regist(UserInfo userInfo) {
        String digestAsHex = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(digestAsHex);
        userInfo.setName(userInfo.getLoginName());
        userInfo.setNickName(userInfo.getLoginName());
        userInfo.setEmail(userInfo.getLoginName()+"qq.com");
        return String.valueOf(userMapper.insert(userInfo));
    }

    @Override
    public UserInfo getUserInfoByUserName(String userName) {
        UserInfo userInfo = new UserInfo();
        userInfo.setLoginName(userName);
        return userMapper.selectOne(userInfo);
    }
}
