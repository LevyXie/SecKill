package top.levygo.seckill.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.levygo.seckill.service.UserService;
import top.levygo.seckill.entity.vo.LoginVo;
import top.levygo.seckill.entity.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * @description：登录Controller
 * @author：LevyXie
 * @create：2022-04-11 15:45
 */
@Controller
@RequestMapping("/login")
@Slf4j
public class LoginController {

    @Autowired
    private UserService userService;

    @ApiOperation("跳转登陆页面")
    @RequestMapping("/toLogin")
    public String login(){
        return "login";
    }

    @ApiOperation("校验登录")
    @RequestMapping("/doLogin")
    @ResponseBody
    public RespBean login(@Valid LoginVo loginVo, HttpServletRequest request, HttpServletResponse response){
        RespBean r = userService.doLogin(loginVo,request,response);
        return r;
    }

}
