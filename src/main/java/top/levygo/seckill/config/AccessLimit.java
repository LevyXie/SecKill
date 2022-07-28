package top.levygo.seckill.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description：秒杀controller的注解，通过Interceptor实现
 * @author：LevyXie
 * @create：2022-04-14 22:43
 */
@Retention(RetentionPolicy.RUNTIME)//运行时
@Target(ElementType.METHOD)//设置为方法的注解
public @interface AccessLimit {
    int second() default 5;
    int maxCount() default 5;
    boolean needLogin();
}
