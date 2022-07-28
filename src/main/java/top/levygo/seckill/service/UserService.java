package top.levygo.seckill.service;

import top.levygo.seckill.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import top.levygo.seckill.entity.vo.LoginVo;
import top.levygo.seckill.entity.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Levy
 * @since 2022-04-11
 */
public interface UserService extends IService<User> {

    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    User getUserByCookie(String token,HttpServletRequest request, HttpServletResponse response);

    RespBean updatePassword(String token,String password,HttpServletRequest request,HttpServletResponse response);
}
