package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-04 11:11
 */
@Controller
public class ItemController {
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    @LoginRequire(autoRedirect = false)
    public String getSkuInfo(@PathVariable String skuId, Model model){
        //根据skuId查询skuInfo和图片信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //根据skiInfo中的skuId和spuId查询spuSaleAttr集合
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        //根据spuId查询所有sku对应的销售属性值集合，将其结果存入map
        List<SkuSaleAttrValue> skuSaleAttrValues = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        HashMap<String, String> map = new HashMap<>();
        String key = "";
        for (int i = 0; i < skuSaleAttrValues.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValues.get(i);
            //准备拼接字符串形式为{"117|119":"33","118|121":"34","117|120":"35"}
            //什么时候拼接| : 当key长度不为0时
            //什么时候结束拼接：当后面一个的skuId和当前这个不一样或循环结束时
            if (key.length()>0){
                key += "|";
            }
            key += skuSaleAttrValue.getSaleAttrValueId();
            if (i + 1 == skuSaleAttrValues.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValues.get(i+1).getSkuId())){
                map.put(key, skuSaleAttrValue.getSkuId());
                key = "";
            }
        }

        String valuesSkuJson  = JSON.toJSONString(map);

        model.addAttribute("valuesSkuJson",valuesSkuJson);

        model.addAttribute("spuSaleAttrList",spuSaleAttrList);

        model.addAttribute("skuInfo", skuInfo);
        listService.hostScore(skuId);
        return "item";
    }
}
