package top.levygo.seckill.config;

import top.levygo.seckill.entity.User;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-14 22:53
 */
public class UserContext {

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user){
        userHolder.set(user);
    }

    public static User getUser(){
        return userHolder.get();
    }
}
