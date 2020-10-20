package com.atguigu.gmall.gmallusermanage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.DigestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallUserManageApplicationTests {

    @Test
    public void contextLoads() {
        String string = DigestUtils.md5DigestAsHex("123".getBytes());
        System.out.println("-----------"+string);
    }

}
