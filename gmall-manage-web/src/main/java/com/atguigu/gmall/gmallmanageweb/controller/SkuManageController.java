package com.atguigu.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-03 10:32
 */
@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }

    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(SpuSaleAttr spuSaleAttr){
        return manageService.getSpuSaleAttrList(spuSaleAttr);
    }

    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        if (skuInfo != null){
            System.out.println("***************" + JSONObject.toJSONString(skuInfo));
            String skuId = manageService.saveSkuInfo(skuInfo);
            SkuInfo skuInfo1 = manageService.getSkuInfo(skuId);
            SkuLsInfo skuLsInfo = new SkuLsInfo();
            BeanUtils.copyProperties(skuInfo1, skuLsInfo);
            listService.saveSkuInfo(skuLsInfo);
        }
    }

}
