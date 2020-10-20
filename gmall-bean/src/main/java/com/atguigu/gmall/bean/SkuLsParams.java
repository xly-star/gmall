package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xulingyun
 * @create 2020-10-09 7:30
 */
@Data
public class SkuLsParams implements Serializable {
    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;

}
