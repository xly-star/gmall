package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author xulingyun
 * @create 2020-10-06 11:52
 */
public class RedisUtil {
    private JedisPool jedisPool;

    //初始化JedisPool
    public void initJedisPool(String host,Integer port,Integer database){
        JedisPoolConfig config = new JedisPoolConfig();
        //连接池最大连接数
        config.setMaxTotal(200);
        //连接池最大预留数，就是不使用的连接数
        config.setMaxIdle(10);
        //如果到最大数，设置等待
        config.setBlockWhenExhausted(true);
        //等待毫秒数
        config.setMaxWaitMillis(10*1000);
        //在获取连接是检查是否有效
        config.setTestOnBorrow(true);
        jedisPool = new JedisPool(config,host,port,20*1000);
    }

    public Jedis getJedis(){
        return jedisPool.getResource();
    }
}
