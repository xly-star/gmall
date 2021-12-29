package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

/**
 * @author xulingyun
 */
public interface ManageService {

    /**
     * 得到一级分类
     * @return
     */
    public List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类id得到二级分类
     * @param catalog1Id
     * @return
     */
    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类得到三级分类
     * @param catalog2Id
     * @return
     */
    public List<BaseCatalog3> getCatalog3(String catalog2Id);


    /**
     * 根据三级分类得到平台属性
     * @param catalog3Id
     * @return
     */
    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 保存和修改平台属性和属性值
     * @param baseAttrInfo
     */
    void saveAttrInfoAndValue(BaseAttrInfo baseAttrInfo);


    /**
     * 根据平台属性id获取平台属性和属性值进行修改回显
     * @param attrId
     * @return
     */
    BaseAttrInfo  getAttrValueList(String attrId);

    /**
     * 根据spuInfo中三级分类id获取所有spuInfo
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getAllSpuInfo(SpuInfo spuInfo);

    /**
     * 得到全部基本销售属性
     * @return
     */
    List<BaseSaleAttr> baseSaleAttrList();


    /**
     * 根据传入的spuInfo保存信息
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuImage中Id属性查询SpuImage集合回显
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);


    /**
     * 根据SpuSaleAttr中的spuId查询SpuSaleAttr集合
     * @param spuSaleAttr
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(SpuSaleAttr spuSaleAttr);


    /**
     * 根据三级分类id查询平台属性和属性值
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrListAndAttrValue(String catalog3Id);


    /**
     *
     * 保存skuInfo
     * @param skuInfo
     */
    String saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据skuId查询SkuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据skiInfo中的skuId和spuId查询spuSaleAttr集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询所有sku对应的销售属性值集合，将其结果存入map
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);


    /**
     * 根据平台属性值id集合查询平台属性值和平台属性集合
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
