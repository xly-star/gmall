package com.atguigu.gmall.gmallcartservice.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-10-14 15:02
 */
public interface CartInfoMapper extends Mapper<CartInfo> {
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
