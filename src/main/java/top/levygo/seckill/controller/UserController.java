package top.levygo.seckill.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.rabbitmq.MQSender;
import top.levygo.seckill.service.UserService;
import top.levygo.seckill.utils.MD5Util;
import top.levygo.seckill.entity.vo.RespBean;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Levy
 * @since 2022-04-11
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MQSender mqSender;


    //==================以下为生成测试用的用户数据================
    //测试，用户信息
    @RequestMapping("/info")
    public RespBean info(User user){
        return RespBean.success(user);
    }

    //测试，生成5000用户
    @RequestMapping("/createUser")
    public RespBean createUser(){
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < 5000; i++) {
            User user = new User();
            user.setMobile(String.valueOf(13000000000L + i));
            user.setNickname("user" + i);
            user.setSalt("1a2b3c4d");
            user.setPassword(MD5Util.inputPassToDBPass("123456", user.getSalt()));
            user.setRegisterDate(new Date());
            user.setLastLoginDate(new Date());
            user.setLoginCount(0);
            users.add(user);
        }
        for (User user : users) {
            userService.save(user);
        }
        return RespBean.success();
    }

    //测试，生成5000用户的登录cookie
    @RequestMapping("/getTestCookies")
    public RespBean getTestCookies(){
        String urlStr = "http://localhost:8889/login/doLogin";
        File file = new File("C:\\Users\\Levy\\Desktop\\cookies.txt");
        if(file.exists()) file.delete();
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(0);
            List<User> users = userService.list(null);
            for (User user : users) {
                URL url = new URL(urlStr);
                HttpURLConnection co = (HttpURLConnection) url.openConnection();
                co.setRequestMethod("POST");
                co.setDoOutput(true);
                OutputStream ops = co.getOutputStream();
                String params = "mobile=" + user.getMobile() + "&password=" + MD5Util.inputPasswordToFormPass("123456");
                ops.write(params.getBytes());
                ops.flush();
                InputStream is = co.getInputStream();
                ByteArrayOutputStream bops = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while((len = is.read(buffer)) >= 0){
                    bops.write(buffer,0,len);
                }
                is.close();
                bops.close();

                String response = new String(bops.toByteArray());
                System.out.println(response);
                ObjectMapper mapper = new ObjectMapper();
                RespBean respBean = mapper.readValue(response, RespBean.class);
                String userToken = (String) respBean.getObj();
                String row = user.getMobile() + "," + userToken;
                raf.seek(raf.length());
                raf.write(row.getBytes());
                raf.write("\r\n".getBytes());
            }
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RespBean.success();
    }

    //=====================以下为对RabbitMQ的测试=================
    //测试发送mq消息
    @RequestMapping("/mq")
    public void mq(){
        mqSender.sendToWorkQueue("Hello");
    }

    //测试发送mq-Fanout消息
    @RequestMapping("/mq/fanout")
    public void mqFanout(){
        mqSender.sendToFanOutExchange("Hello");
    }

    //测试发送mq-Direct消息
    @RequestMapping("/mq/direct01")
    public void mqDirect01(){
        mqSender.sendToDirectExchange01("Hello,Red");
    }

    //测试发送mq-Direct消息
    @RequestMapping("/mq/direct02")
    public void mqDirect02(){
        mqSender.sendToDirectExchange02("Hello,Blue");
    }

    //测试发送mq-Topic消息
    @RequestMapping("/mq/topic01")
    public void mqTopic01(){
        mqSender.sendToTopicExchange01("Hello,only one");
    }

    //测试发送mq-Topic消息
    @RequestMapping("/mq/topic02")
    public void mqTopic02(){
        mqSender.sendToTopicExchange02("Hello,you two");
    }
}

