package top.levygo.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.entity.vo.RespBean;
import top.levygo.seckill.entity.vo.RespBeanEnum;
import top.levygo.seckill.service.UserService;
import top.levygo.seckill.utils.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @description：拦截器实现AccessLimit的注解功能
 * @author：LevyXie
 * @create：2022-04-14 22:45
 */
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            User user = getUser(request,response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod)handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            //判断是否带注解，有该注解则开启拦截
            if(null == accessLimit) {
                return true;
            }
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            String uri = request.getRequestURI();
            if(needLogin){
                //判断用户登录
                if(null == user){
                    render(response, RespBeanEnum.SESSION_ERROR);
                    return false;
                }
                //判断用户是否重复刷新，过于频繁
                String key = uri + ":" + user.getId();
                ValueOperations valueOperations = redisTemplate.opsForValue();
                Integer count = (Integer) valueOperations.get(key);
                if(null == count){
                    valueOperations.set(uri + ":" + user.getId(), 1,maxCount, TimeUnit.SECONDS);
                }else if(count < second){
                    valueOperations.increment(uri + ":" + user.getId());
                }else {
                    render(response, RespBeanEnum.REPEAT_REQUEST);
                    return false;
                }
            }
            
        }
        return true;
    }

    //构建返还对象
    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        writer.write(new ObjectMapper().writeValueAsString(respBean));
        writer.flush();
        writer.close();
    }

    //获取用户对象
    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtil.getCookieValue(request, "userToken");
        if(StringUtils.isEmpty(token)){
            return null;
        }
        return userService.getUserByCookie(token, request, response);
    }
}
