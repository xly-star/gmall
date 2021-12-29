package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xulingyun
 * @create 2021-12-12 16:51
 */
@Data
public class SaleStatistics implements Serializable {

    private static final long serialVersionUID = -6849794470756342340L;

    /**
     * 开始时间
     */
    private String begin;
    /**
     * 结束时间
     */
    private String end;
    /**
     * 三级分类id
     */
    private String catalog3Id;
}
