package top.levygo.seckill.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.service.UserService;
import top.levygo.seckill.utils.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @description：用户自定义参数
 * @author：LevyXie
 * @create：2022-04-11 23:15
 */
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        //判断入参是否为User,为true则执行下面的resolveArgument方法
        Class<?> clazz = methodParameter.getParameterType();
        return clazz == User.class;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
//        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
//        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
//        String token = CookieUtil.getCookieValue(request, "userToken");
//        if(StringUtils.isEmpty(token)){
//            return null;
//        }
//        User user = userService.getUserByCookie(token, request, response);
//        return user;
        //以上功能已被拦截器实现，并放入ThreadLocal线程中,只需通过UserContext对象即可获得
        return UserContext.getUser();
    }
}
