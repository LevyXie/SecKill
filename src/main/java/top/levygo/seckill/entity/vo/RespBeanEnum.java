package top.levygo.seckill.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-11 15:58
 */
@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常"),
    LOGIN_ERROR(500210,"用户名或密码错误"),
    MOBILE_ERROR(500211,"手机号码格式错误"),
    BIND_ERROR(500212,"参数校验异常"),
    EMPTY_STOCK_ERROR(500215,"库存为空"),
    ALREADY_BUY_ERROR(50016,"用户已经参与秒杀"),
    UPDATE_PASSWORD_ERROR(50017,"更新密码失败"),
    SESSION_ERROR(50018,"用户不存在"),
    ORDER_ERROR(50019,"订单不存在"),
    ILLEGAL_REQUEST(50020,"非法请求，请重试"),
    WRONG_CAPTCHA(50021,"验证码错误，请重新输入"),
    REPEAT_REQUEST(50022,"访问过于频繁，请稍后重试");


    private final Integer code;
    private final String message;

}
