package com.atguigu.gmall.gmallmanageservice.service.impl;

import com.google.common.collect.Maps;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.gmallmanageservice.mapper.SkuInfoMapper;
import com.atguigu.gmall.gmallmanageservice.mapper.SpuInfoMapper;
import com.atguigu.gmall.gmallorderservice.mapper.OrderDetailMapper;
import com.atguigu.gmall.gmallorderservice.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xulingyun
 * @create 2021-12-12 16:48
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    private Random random = new Random();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public Map<String, Object> getEChartsByDate(SaleStatistics saleStatistics) {
        Map<String, Object> res = new HashMap<>();
        //根据三级分类id获取所有spu信息
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(saleStatistics.getCatalog3Id());

        List<SpuInfo> spuList = spuInfoMapper.select(spuInfo);

        //获取三级分类下所有spuId
        List<String> spuIdList = spuList.stream().map(SpuInfo::getId).collect(Collectors.toList());

        //获取所有三级分类下的sku信息
        Example skuInfoExample = new Example(SkuInfo.class);
        skuInfoExample.createCriteria().andIn("spuId", spuIdList);
        List<SkuInfo> skuInfoList = skuInfoMapper.selectByExample(skuInfoExample);

        //获取所有三级类目下的skuId
        List<String> skuIdList = skuInfoList.stream().map(SkuInfo::getId).collect(Collectors.toList());

        Example orderDetailExample = new Example(OrderDetail.class);
        orderDetailExample.createCriteria().andIn("skuId", skuIdList);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectByExample(orderDetailExample);

        //返回结果集
        //每天销售价格
        Map<String, BigDecimal> priceMap = new HashMap<>();
        //每日销售数目
        Map<String, Integer> countMap = new HashMap<>();
        //根据订单信息查询订单日期
        //todo 限制时间
        for (OrderDetail orderDetail : orderDetailList) {
            Example orderInfoExample = new Example(OrderInfo.class);
            try {
                orderInfoExample.createCriteria()
                        .andBetween("createTime", sdf.parse(saleStatistics.getBegin()), getLastDate(saleStatistics.getEnd()))
                        .andEqualTo("id", orderDetail.getOrderId());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            OrderInfo orderInfo = orderInfoMapper.selectOneByExample(orderInfoExample);
            if (Objects.isNull(orderInfo)) {
                continue;
            }

            String date = sdf.format(orderInfo.getCreateTime());
            priceMap.put(date, priceMap.getOrDefault(date, new BigDecimal(0)).add(orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum()))));
            countMap.put(date, countMap.getOrDefault(date, 0) + orderDetail.getSkuNum());
        }

        Set<String> calculateSet = countMap.keySet();

        ArrayList<String> calculateList = new ArrayList<>(calculateSet);
        Object[] priceList = priceMap.values().toArray(new Object[priceMap.size()]);
        Object[] countList = countMap.values().toArray(new Object[countMap.size()]);

        //根据时间对数据排序
        quickSort(calculateList, priceList, countList, 0, calculateList.size() - 1);

        res.put("calculateList", calculateList);
        res.put("priceList", priceList);
        res.put("countList", countList);
        return res;
    }
    public Date getLastDate(String end) throws ParseException {
        end += " 23:59:59";
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return s.parse(end);
    }

    public static void main(String[] args) {
//        ArrayList<String> strings = new ArrayList<>();
//        strings.add("2018-09-10");//4
//        strings.add("2017-09-10");//3
//        strings.add("2019-09-10");//6
//        strings.add("2016-09-10");//1
//        strings.add("2018-10-10");//5
//        strings.add("2017-01-10");//2
        StatisticsServiceImpl statisticsService = new StatisticsServiceImpl();
//        Object[] objects = {4, 3, 6, 1, 5, 2};
//        Object[] objects1 = {4, 3, 6, 1, 5, 2};
//        statisticsService.quickSort(strings, objects, objects1, 0, strings.size() - 1);
//        System.out.println(strings);
//        System.out.println(Arrays.toString(objects));
//        System.out.println(Arrays.toString(objects1));

        String s = "2019-09-09";
        Date date = new Date();
        try {
            String format = statisticsService.sdf.format(date);
            System.out.println(format);
            System.out.println(statisticsService.sdf.parse(format));
            System.out.println(statisticsService.sdf.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void quickSort(List<String> calculateList, Object[] priceList, Object[] countList, int left, int right) {
        int l = left, r = right;
        String tmp;
        Object pTmp;
        Object cTmp;
        int t = l + random.nextInt(r - l + 1);
        tmp = calculateList.get(t);
        calculateList.set(t, calculateList.get(l));
        calculateList.set(l, tmp);

        pTmp = priceList[t];
        priceList[t] = priceList[l];
        priceList[l] = pTmp;

        cTmp = countList[t];
        countList[t] = countList[l];
        countList[l] = cTmp;

        String p = calculateList.get(l);
        int j = l;
        for (int i = l + 1; i <= r; i++) {
            if (calculateList.get(i).compareTo(p) < 0) {
                j++;
                tmp = calculateList.get(i);
                calculateList.set(i, calculateList.get(j));
                calculateList.set(j, tmp);

                pTmp = priceList[i];
                priceList[i] = priceList[j];
                priceList[j] = pTmp;

                cTmp = countList[i];
                countList[i] = countList[j];
                countList[j] = cTmp;
            }
        }

        tmp = calculateList.get(j);
        calculateList.set(j, calculateList.get(l));
        calculateList.set(l, tmp);

        pTmp = priceList[j];
        priceList[j] = priceList[l];
        priceList[l] = pTmp;

        cTmp = countList[j];
        countList[j] = countList[l];
        countList[l] = cTmp;

        if (left < j - 1) {
            quickSort(calculateList, priceList, countList, left, j - 1);
        }
        if (j + 1 < right) {
            quickSort(calculateList, priceList, countList, j + 1, right);
        }
    }

}
