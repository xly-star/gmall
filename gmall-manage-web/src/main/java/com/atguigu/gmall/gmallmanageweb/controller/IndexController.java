package com.atguigu.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author xulingyun
 * @create 2021-11-21 16:04
 */
@Controller
public class IndexController {
    @RequestMapping("/index")
    public String index(){
        System.out.println("访问首页***");
        return "index";
    }

    @RequestMapping("logout")
    public String logout(HttpServletRequest request,HttpServletResponse response){
        CookieUtil.deleteCookie(request,response,"token");
        return "redirect:http://www.gmall.com/index";
    }
}
