package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xulingyun
 * @create 2020-10-08 18:40
 */
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        try {
            Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
            DocumentResult result = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //根据传入参数编写dsl语句
        SkuLsResult skuLsResult = null;
        try {
            String query = makeQueryStringForSearch(skuLsParams);
            Search search = new Search.Builder(query).addIndex("gmall").addType("SkuInfo").build();
            SearchResult result = jestClient.execute(search);
            skuLsResult = makeResultForSearch(result, skuLsParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return skuLsResult;
    }

    @Override
    public void hostScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        //将热度传入es中
        if (hotScore % 10 == 0) {
            updateES(Math.round(hotScore), skuId);
        }
    }

    private void updateES(long hotScore, String skuId) {
        try {
            //定义dsl语句
            String upd = "{\n" +
                    "  \"doc\": {\n" +
                    "    \"hotScore\":" + hotScore + "\n" +
                    "  }\n" +
                    "}";
            //定义执行方法
            Update build = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
            //执行
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private SkuLsResult makeResultForSearch(SearchResult result, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
//        List<SkuLsInfo> skuLsInfoList;
        ArrayList<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = result.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo source = hit.source;
            //设置高亮属性
            if (hit.highlight != null && hit.highlight.size() > 0) {
                Map<String, List<String>> highlight = hit.highlight;
                List<String> skuName = highlight.get("skuName");
                source.setSkuName(skuName.get(0));
            }
            skuLsInfoList.add(source);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
//        long total;
        skuLsResult.setTotal(result.getTotal());
//        long totalPages;
        Long totalPage = (result.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

//        List<String> attrValueIdList;
        ArrayList<String> attrValueIdList = new ArrayList<>();
        MetricAggregation aggregations = result.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            attrValueIdList.add(bucket.getKey());
        }
        skuLsResult.setAttrValueIdList(attrValueIdList);
        return skuLsResult;
    }

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //搜索器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //设置搜索字段
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            MatchQueryBuilder skuName = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(skuName);
            //设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("skuName");
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //设置三级分类
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            //创建term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //创建filter
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //设置属性值
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            String[] valueId = skuLsParams.getValueId();
            for (String value : valueId) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", value);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //创建query
        searchSourceBuilder.query(boolQueryBuilder);

        //分页
        Integer from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr");
        groupby_attr.field("skuAttrValueList.valueId");
        //设置聚合属性的数量
        groupby_attr.size(100);
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        System.out.println("query=" + query);
        return query;
    }
}
