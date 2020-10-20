package com.atguigu.gmall.gmallmanageservice.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.gmallmanageservice.constant.ManageConst;
import com.atguigu.gmall.gmallmanageservice.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author xulingyun
 * @create 2020-10-01 8:38
 */
@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCatalog1Mapper catalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper catalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper catalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper attrInfoMapper;

    @Autowired
    private BaseAttrValueMapper attrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;


    @Autowired
    private BaseSaleAttrMapper saleAttrMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return catalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return catalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return catalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        return attrInfoMapper.select(baseAttrInfo);
    }


    @Override
    @Transactional
    public void saveAttrInfoAndValue(BaseAttrInfo baseAttrInfo) {
        if (!StringUtils.isEmpty(baseAttrInfo.getId())) {
            //修改平台属性
            attrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {
            //保存平台属性
            attrInfoMapper.insert(baseAttrInfo);
        }

        //根据平台属性id平台属性值先清空在添加
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        attrValueMapper.delete(baseAttrValueDel);

        for (BaseAttrValue baseAttrValue : baseAttrInfo.getAttrValueList()) {
            baseAttrValue.setId(null);
            baseAttrValue.setAttrId(baseAttrInfo.getId());
            attrValueMapper.insert(baseAttrValue);
        }
    }

    @Override
    public BaseAttrInfo getAttrValueList(String attrId) {
        BaseAttrInfo baseAttrInfo = attrInfoMapper.selectByPrimaryKey(attrId);
        if (baseAttrInfo != null) {
            BaseAttrValue baseAttrValue = new BaseAttrValue();
            baseAttrValue.setAttrId(baseAttrInfo.getId());
            baseAttrInfo.setAttrValueList(attrValueMapper.select(baseAttrValue));
            return baseAttrInfo;
        } else {
            return null;
        }
    }

    @Override
    public List<SpuInfo> getAllSpuInfo(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return saleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insertSelective(spuInfo);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(SpuSaleAttr spuSaleAttr) {
        return spuSaleAttrMapper.getSpuSaleAttrList(spuSaleAttr.getSpuId());
    }

    @Override
    public List<BaseAttrInfo> getAttrListAndAttrValue(String catalog3Id) {
        return attrInfoMapper.getAttrListAndAttrValue(catalog3Id);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存SkuInfo
        skuInfoMapper.insertSelective(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        //保存SkuImage
        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }

        //保存SkuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        //保存skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
//        return getSkuInfoByNoRedis(skuId);
//        return getSkuInfoByRedis(skuId);
//        return getSkuInfoRedisSetNxAndEx(skuId);
        return getSkuInfoByRedisson(skuId);
    }

    private SkuInfo getSkuInfoByRedisson(String skuId) {
        RLock lock = null;
        SkuInfo skuInfo;
        Jedis jedis = null;
        try {
            //Redisson底层使用的也是setnx和setex
            Config config = new Config();
            config.useSingleServer().setAddress("redis://192.168.10.128:6379");
            RedissonClient redissonClient = Redisson.create(config);
            lock = redissonClient.getLock("yourLock");
            lock.lock(10, TimeUnit.SECONDS);
            jedis = redisUtil.getJedis();
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(skuKey)) {
                String skuJson = jedis.get(skuKey);
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            } else {
                skuInfo = getSkuInfoByNoRedis(skuId);
                jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if(lock != null) {
                lock.unlock();
            }
        }
        //当redis宕机后从数据库拿数据
        return getSkuInfoByNoRedis(skuId);
    }

    private SkuInfo getSkuInfoRedisSetNxAndEx(String skuId) {
        //解决缓存击穿问题 ：当redis中的一个key失效时有很多请求同时访问这个key导致全部访问数据库区，
        // 数据库可能会蹦掉
        //解决方法加锁：分布式锁 当一个redis中的key失效时让一个请求去数据库中查找数据并加入redis，
        //其他请求等待，然后访问redis是中新加入的数据 使用setnx和setex
        SkuInfo skuInfo;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(skuKey);
            String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
            if (skuJson == null || skuJson.length() == 0) {
                System.out.println("_____________没有命中缓存");
                //当redis中没有数据时先加锁 nx代表只有不存在才会设置 px代表过期毫秒数
                String lockKey = jedis.set(skuLockKey, "locked", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)) {
                    //表示加锁成功
                    skuInfo = getSkuInfoByNoRedis(skuId);
                    jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    jedis.del(skuLockKey);
                    return skuInfo;
                } else {
                    //等待1秒其他请求
                    Thread.sleep(1000);
                    //回调
                    getSkuInfo(skuId);
                }
            } else {
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        //当redis宕机后从数据库拿数据
        return getSkuInfoByNoRedis(skuId);
    }

    private SkuInfo getSkuInfoByRedis(String skuId) {
        SkuInfo skuInfo;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(skuKey)) {
                String skuJson = jedis.get(skuKey);
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            } else {
                skuInfo = getSkuInfoByNoRedis(skuId);
                jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        //当redis宕机后从数据库拿数据
        return getSkuInfoByNoRedis(skuId);
    }

    private SkuInfo getSkuInfoByNoRedis(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        if (skuInfo != null) {
//            Jedis jedis = redisUtil.getJedis();
//            jedis.set("ok", "没毛病");
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
            skuInfo.setSkuAttrValueList(skuAttrValueList);
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        System.out.println("valueIds=" + valueIds);
        return attrInfoMapper.selectAttrList(valueIds);
    }
}
