package com.atguigu.gmall.gmalllistweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
//    @ResponseBody
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {
        //设置页面大小
        skuLsParams.setPageSize(12);
        SkuLsResult search = listService.search(skuLsParams);
        List<SkuLsInfo> skuLsInfoList = search.getSkuLsInfoList();
        List<String> attrValueIdList = search.getAttrValueIdList();
        if (attrValueIdList != null && attrValueIdList.size() > 0) {
            //存放面包屑的集合
            ArrayList<BaseAttrValue> attrValueArrayList = new ArrayList<>();
            List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
            //删除被选中的平台属性
            for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                        for (String valueId : skuLsParams.getValueId()) {
                            if (valueId.equals(baseAttrValue.getId())) {
                                iterator.remove();
                                //构造面包屑
                                BaseAttrValue attrValue = new BaseAttrValue();
                                attrValue.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                                String urlParam = makeUrlParam(skuLsParams, valueId);
                                attrValue.setUrlParam(urlParam);
                                attrValueArrayList.add(attrValue);
                            }
                        }
                    }
                }
            }
            request.setAttribute("attrList", attrList);
            request.setAttribute("attrValueArrayList", attrValueArrayList);
        }

        //分页
        request.setAttribute("totalPages", search.getTotalPages());
        request.setAttribute("pageNo", skuLsParams.getPageNo());
        //拼接url?后的参数
        String urlParam = makeUrlParam(skuLsParams);
        request.setAttribute("keyword", skuLsParams.getKeyword());
        request.setAttribute("urlParam", urlParam);
        request.setAttribute("skuLsInfoList", skuLsInfoList);
        return "list";
    }

    /**
     * 拼接url?后的参数
     *
     * @param skuLsParams
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) {
        String urlParam = "";
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            if (urlParam.length() > 0) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (String valueId : skuLsParams.getValueId()) {
                if (excludeValueIds != null && excludeValueIds.length > 0) {
                    String id = excludeValueIds[0];
                    if (valueId.equals(id)) {
                        continue;
                    }
                }
                if (urlParam.length() > 0) {
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }
        System.out.println("urlParam=" + urlParam);
        return urlParam;
    }
}
