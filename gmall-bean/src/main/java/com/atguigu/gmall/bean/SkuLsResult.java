package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-09 7:31
 */
@Data
public class SkuLsResult implements Serializable {
    List<SkuLsInfo> skuLsInfoList;

    long total;

    long totalPages;

    List<String> attrValueIdList;

}
