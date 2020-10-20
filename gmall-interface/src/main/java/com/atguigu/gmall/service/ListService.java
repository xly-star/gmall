package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

/**
 * @author xulingyun
 * @create 2020-10-08 18:39
 */
public interface ListService {
    /**
     * 保存skuInfo到es上
     * @param skuLsInfo
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 根据传入参数值检索商品列表
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 根据skuId在redis中增加该商品的浏览次数，当skuId到达100时存入es中，要是一直存入es产生大量io消耗性能大
     * @param skuId
     */
    void hostScore(String skuId);
}
