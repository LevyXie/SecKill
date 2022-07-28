package top.levygo.seckill.entity.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import top.levygo.seckill.validator.IsMobile;

import javax.validation.constraints.NotNull;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-11 16:16
 */
@Data
public class LoginVo {
    @NotNull
    @IsMobile//自定义的注解，详见validator包
    private String mobile;
    @NotNull
    @Length(min = 32)//MD5加密后的密码长度，至少32位
    private String password;
}
