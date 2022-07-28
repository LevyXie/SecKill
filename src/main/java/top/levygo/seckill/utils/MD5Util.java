package top.levygo.seckill.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

/**
 * @description：MD5对用户明文密码加密
 * @author：LevyXie
 * @create：2022-04-11 11:32
 */
@Component
public class MD5Util {
    //加密salt
    private static final String salt = "1a2b3c4d";

    //MD5加密
    public static String md5(String src){
        return DigestUtils.md5Hex(src);
    }

    //一次加密
    public static String inputPasswordToFormPass(String inputPass){
        //为了安全性，从salt中取值
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    //二次加密
    public static String formPassToDBPass(String formPass,String salt){
        String str = "" + salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    //对外暴露，加密使用的方法
    public static String inputPassToDBPass(String inputPass,String salt){
        String formPass = inputPasswordToFormPass(inputPass);
        String dbPass = formPassToDBPass(formPass, salt);
        return dbPass;
    }

    public static void main(String[] args) {
        String str = "123456";
        System.out.println(inputPasswordToFormPass(str));
        System.out.println(formPassToDBPass("d3b1294a61a07da9b49b6e22b2cbd7f9", "1a2b3c4d"));
        System.out.println(inputPassToDBPass(str, "1a2b3c4d"));
    }
}
