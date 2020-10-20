package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author xulingyun
 * @create 2020-10-03 12:15
 */
@Data
public class SkuImage implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String skuId;

    @Column
    private String imgName;

    @Column
    private String imgUrl;

    @Column
    private String spuImgId;

    @Column
    private String isDefault;
}
