package top.levygo.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @description：Redis分布式锁的测试
 * @author：LevyXie
 * @create：2022-04-14 16:12
 */

@SpringBootTest
public class RedisLockTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisScript script;

    @Test
    public void redisTest1(){//基本的setnx
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1");//判断是否被锁
        //未锁
        if(isLock){
            valueOperations.set("name", "kakaka");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //处理完成，删除锁
            redisTemplate.delete("k1");
        }else {
            System.out.println("线程正在使用，请稍后再试....");
        }
    }

    @Test//异常情况下，通过超时时间进行限制
    public void redisTest2(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1",3, TimeUnit.SECONDS);
        //未锁
        if(isLock){
            int i = 1 / 0;
            valueOperations.set("name", "kakaka");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //处理完成，删除锁
            redisTemplate.delete("k1");
        }else {
            System.out.println("线程正在使用，请稍后再试...");
        }
    }

    @Test//多重操作情况下采用lua脚本保证原子性
    public void redisTest3(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String value = UUID.randomUUID().toString();
        Boolean isLock = valueOperations.setIfAbsent("k1", value, 3, TimeUnit.SECONDS);
        if(isLock){
            valueOperations.set("name", "kakaka");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            //原子性的lua脚本判断是否为自己的锁
            Boolean result = (Boolean) redisTemplate.execute(script, Collections.singletonList("k1"), value);
            System.out.println(result);
        }else {
            System.out.println("线程正在使用，请稍后再试...");
        }

    }
}
