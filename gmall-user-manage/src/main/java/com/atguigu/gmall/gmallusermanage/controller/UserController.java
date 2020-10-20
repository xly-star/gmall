package com.atguigu.gmall.gmallusermanage.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author xulingyun
 * @create 2020-09-28 22:12
 */
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping("findAllUser")
    public List<UserInfo> findAllUser(){
        return userService.getAllUserInfo();
    }
}
