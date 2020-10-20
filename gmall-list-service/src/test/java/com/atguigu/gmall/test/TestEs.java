package com.atguigu.gmall.test;

import com.atguigu.gmall.list.ListApplication;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author xulingyun
 * @create 2020-10-08 18:14
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ListApplication.class)
public class TestEs {

    @Autowired
    private JestClient jestClient;

    @Test
    public void testes() throws IOException {
        //1.确定dsl语句
        String query = "{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"name\": \"湄公河\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        //2.确定执行的方式
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
        //3.执行
        SearchResult execute = jestClient.execute(search);
        System.out.println(execute);
        System.out.println(execute.getHits(Map.class));
        //4.获取返回结果集
        List<SearchResult.Hit<Map, Void>> hits = execute.getHits(Map.class);
        for (SearchResult.Hit<Map, Void> hit : hits) {
            Map source = hit.source;
            System.out.println(source.get("name"));
        }
    }
}
