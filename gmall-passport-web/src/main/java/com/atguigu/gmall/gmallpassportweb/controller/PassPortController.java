package com.atguigu.gmall.gmallpassportweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.gmallpassportweb.util.JWTUtil;
import com.atguigu.gmall.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xulingyun
 * @create 2020-10-10 16:22
 */
@Controller
public class PassPortController {
    @Reference
    private UserService userService;


    @Value("${token.key}")
    private String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        //获取ip地址
        String salt = request.getHeader("X-forwarded-for");
        UserInfo info = userService.login(userInfo);
        if (info != null) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", info.getId());
            map.put("nickName", info.getNickName());
            String token = JWTUtil.encode(key, map, salt);
            return token;
        } else {
            return "fail";
        }
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String salt) {
        //token = eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.-Zn1x_RSf8n4V7hM1bxp5aoIsOJZTVBzboU6d2mbNe0
        //salt = 192.168.10.1
        Map<String, Object> map = JWTUtil.decode(token, key, salt);
        if (map != null && map.size() > 0) {
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }
}
