package top.levygo.seckill.utils;

import java.util.UUID;

/**
 * @description：生成UUID的简单工具类
 * @author：LevyXie
 * @create：2022-04-11 18:18
 */
public class UUIDUtil {

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
