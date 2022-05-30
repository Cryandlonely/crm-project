package com.bjpowernode.crm.settings.web.controller;

import com.bjpowernode.crm.commons.contants.Contants;
import com.bjpowernode.crm.commons.utils.dateUtils;
import com.bjpowernode.crm.commons.domain.ReturnObject;
import com.bjpowernode.crm.settings.domain.User;
import com.bjpowernode.crm.settings.service.UserService;
import com.sun.deploy.net.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * url要和controller方法处理完请求之后，响应信息返回的页面的资源目录保持一致
     */
    @RequestMapping("/settings/qx/user/toLogin.do")
    public String toLogin() {
        //请求转发到登录页面
        return "settings/qx/user/login";
    }

    @RequestMapping("/settings/qx/user/login.do")
    public @ResponseBody
    Object login(String loginAct, String loginPwd, String isRemPwd, HttpServletResponse response, HttpServletRequest request, HttpSession session) {
        //封装参数
        Map<String, Object> map = new HashMap<>();
        map.put("loginAct", loginAct);
        map.put("loginPwd", loginPwd);
        //调用service层方法，查询用户
        User user = userService.queryUserByLoginActAndPwd(map);

        //根据查询结果，生成响应信息
        ReturnObject returnObject = new ReturnObject();
        if (user == null) {
            //登录失败,用户名或者密码错误
            returnObject.setCode("0");
            returnObject.setMessage("用户名或者密码错误");
        } else {
            if (dateUtils.DateFormat(new Date()).compareTo(user.getExpireTime()) > 0) {
                //登录失败，账号已过期
                returnObject.setCode(Contants.RETURN_OBJECT_CODE_FAILURE);
                returnObject.setMessage("账号已过期");
            } else if ("0".equals(user.getLockState())) {
                //登录失败，状态被锁定
                returnObject.setCode(Contants.RETURN_OBJECT_CODE_FAILURE);
                returnObject.setMessage("状态被锁定");
            } else if (!user.getAllowIps().contains(request.getRemoteAddr())) {
                //登录失败，ip受限
                returnObject.setCode(Contants.RETURN_OBJECT_CODE_FAILURE);
                returnObject.setMessage("ip受限");
            } else {
                //登录成功
                returnObject.setCode(Contants.RETURN_OBJECT_CODE_SUCCESS);
                session.setAttribute(Contants.SESSION_USER, user);
                if ("true".equals(isRemPwd)) {
                    Cookie cookie01 = new Cookie("loginAct", loginAct);
                    cookie01.setMaxAge(10 * 24 * 60 * 60);
                    response.addCookie(cookie01);
                    Cookie cookie02 = new Cookie("loginPwd", loginPwd);
                    cookie02.setMaxAge(10 * 24 * 60 * 60);
                    response.addCookie(cookie02);
                } else {
                    Cookie cookie03 = new Cookie("loginAct", "1");
                    cookie03.setMaxAge(0);
                    response.addCookie(cookie03);
                    Cookie cookie04 = new Cookie("loginPwd", "1");
                    cookie04.setMaxAge(0);
                    response.addCookie(cookie04);
                }
            }
        }

        return returnObject;
    }

    @RequestMapping("settings/user/logout.do")
    public String logOut(HttpServletResponse response, HttpSession session) {
        Cookie cookie01 = new Cookie("loginAct", "1");
        cookie01.setMaxAge(0);
        response.addCookie(cookie01);
        Cookie cookie02 = new Cookie("loginPwd", "1");
        cookie02.setMaxAge(0);
        response.addCookie(cookie02);
        session.invalidate();
        return "redirect:/";
    }
}
