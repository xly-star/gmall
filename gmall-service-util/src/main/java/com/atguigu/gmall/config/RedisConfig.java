package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xulingyun
 * @create 2020-10-06 11:59
 */
@Configuration//相当于.xml文件
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.database}")
    private Integer database;

    @Bean //相当于spring配置文件中的<bean></bean>
    public RedisUtil getRedisUtil(){
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host, port, database);
        return redisUtil;
    }
}

