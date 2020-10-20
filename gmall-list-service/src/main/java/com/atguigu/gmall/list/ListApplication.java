package com.atguigu.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author xulingyun
 * @create 2020-10-08 18:12
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.atguigu.gmall")
public class ListApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListApplication.class,args);
    }
}
