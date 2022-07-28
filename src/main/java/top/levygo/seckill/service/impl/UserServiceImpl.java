package top.levygo.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.exception.GlobalException;
import top.levygo.seckill.mapper.UserMapper;
import top.levygo.seckill.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.levygo.seckill.utils.CookieUtil;
import top.levygo.seckill.utils.MD5Util;
import top.levygo.seckill.utils.UUIDUtil;
import top.levygo.seckill.entity.vo.LoginVo;
import top.levygo.seckill.entity.vo.RespBean;
import top.levygo.seckill.entity.vo.RespBeanEnum;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Levy
 * @since 2022-04-11
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        //以下功能已通过validator注解实现
//        //后端校验
//        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password)){
//            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
//        }
//        if(!ValidatorUtil.isMobile(mobile)){
//            return RespBean.error(RespBeanEnum.MOBILE_ERROR);
//        }
        //查询user
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile", mobile);
        User user = baseMapper.selectOne(wrapper);
        //校验user
        if(null == user){
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        String dbPass = MD5Util.formPassToDBPass(password, user.getSalt());
        if(!dbPass.equals(user.getPassword())){
            throw  new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        /*登录成功，生成cookie并将cookie放入session*/
        //生成cookie
        String token = UUIDUtil.uuid();
//        request.getSession().setAttribute(token, user);//使用redis直接存储信息，不使用session
        redisTemplate.opsForValue().set("user:" + token, user);
        //设置cookie
        CookieUtil.setCookie(request, response, "userToken", token);
        return RespBean.success(token);
    }

    @Override
    public User getUserByCookie(String token,HttpServletRequest request,HttpServletResponse response) {
        if(StringUtils.isEmpty(token)){
            return null;
        }
        User user = (User) redisTemplate.opsForValue().get("user:" + token);
        if(user != null){
            //设置cookie，作为保障的一个措施
            CookieUtil.setCookie(request, response, "userToken", token);
        }
        return user;
    }


    //以下为更新用户密码的service,核心在于更新数据库时,删除redis的中的值,避免redis中值和数据库不一致
    @Override
    public RespBean updatePassword(String token, String password,HttpServletRequest request,HttpServletResponse response) {
        User user = getUserByCookie(token, request, response);
        if(null == user){
            throw new GlobalException(RespBeanEnum.BIND_ERROR);
        }
        user.setPassword(MD5Util.inputPassToDBPass(password, user.getSalt()));
        int i = baseMapper.updateById(user);
        if(i == 1){//更新成功
            redisTemplate.delete("user:" + token);
            return RespBean.success();
        }
        return RespBean.error(RespBeanEnum.UPDATE_PASSWORD_ERROR);
    }
}
