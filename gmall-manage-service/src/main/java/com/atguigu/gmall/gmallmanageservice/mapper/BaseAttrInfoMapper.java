package com.atguigu.gmall.gmallmanageservice.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-01 8:28
 */
public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> selectAttrList(@Param("valueIds") String valueIds);

    List<BaseAttrInfo> getAttrListAndAttrValue(String catalog3Id);
}
