package top.levygo.seckill.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.levygo.seckill.entity.vo.RespBeanEnum;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-11 18:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalException extends RuntimeException{
    private RespBeanEnum respBeanEnum;
}
