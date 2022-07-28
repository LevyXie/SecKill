# SecKill-秒杀系统的设计与实现
经典高并发场景：电商秒杀模块

## 一、设计思路

### 秒杀系统的要求

- 高性能（**应对高并发**）：应对海量并发请求，通过**多层次多粒度的缓存**（redis页面缓存，redis对象缓存，JVM内存标记关键数据），**前后端分离**减轻服务器压力，**消息队列**（RabbitMQ）异步下单实现流量削峰。
- 一致性（**解决超卖问题**）。秒杀中商品减库存的实现方式同样关键。实现原子性操作：一条sql语句为原子性操作，redis分布式锁setnx，lua脚本实现redis的原子性加锁及释放锁。
- 高可用（**接口限流防刷**）。为了应对脚本等高强度、非真实用户的海量并发请求。采用接口限流防刷措施（隐藏秒杀接口，验证码，设置计数限流)；另，应对意外情况，服务器宕机等，采用熔断处理机制（服务降级等，本项目中未实现）。

### 秒杀系统的架构

- 流量入口：商品详情页
- 接口防刷：验证码&隐藏真实地址接口&接口限流（计数限流/令牌限流）
- 静态资源缓存：页面静态化，可置于CDN静态资源缓存。
- Nginx/GateWay/Zuul：负载均衡
- JVM内存：标记关键数据
- Redis预减库存：分布式锁，lua原子性操作
- RabbitMQ异步请求Mysql，异步下单：保持redis和MySQL数据同步。

**架构图一**

<img src="https://myimageserver.oss-cn-beijing.aliyuncs.com/img/kill2.png" alt="Seckill1" style="zoom:80%;" />

**架构图二**

<img src="https://myimageserver.oss-cn-beijing.aliyuncs.com/img/kill1.png" alt="Seckill2" style="zoom:80%;" />

## 二、设计要点

### 1、用户密码两次MD5加密

**设计思路：**

- 前端通过用户输入的明文密码，直接进行一次MD5加密，作为表单提交给后端。

- 后端设计MD5Util，内设置：
  
  - inputPasswordToFormPass ===> 明文密码加密为表单密码：一次加密
  - formPassToDBPass ===> 表单密码加密为数据库密码：二次加密
  - inputPassToDBPass ===> 明文密码直接加密为表单密码

- 后端接收到前端提交的表单密码，formPassToDBPass转换为数据库密码，和数据库密码对比即可。

- 注意：一次加密的salt可在前后端固定，二次加密的salt可直接在注册阶段生成随机数，存入数据库，供后端校验时调取。

**代码实现：**

```java
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
}
```

### 2、分布式Session实现用户登录

> 使用cookie和分布式session实现SSO单点登录

**设计思路：**

- 登录阶段：
  
  redis中存储：key==>UUID生成的token,value===>登录的User对象
  
  cookie中存储token

- 其他页面访问阶段：
  
  设置拦截器，拦截器执行以下操作：
  
  从request的cookie中取出token。
  
  用token从redis中读取User对象。

**代码实现：**

登录阶段

```java
        /*登录成功，生成cookie并将cookie放入session*/
        //生成cookie
        String token = UUIDUtil.uuid();
        redisTemplate.opsForValue().set("user:" + token, user);
        //设置cookie
        CookieUtil.setCookie(request, response, "userToken", token);
```

访问阶段

```java
        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
        String token = CookieUtil.getCookieValue(request, "userToken");
        if(StringUtils.isEmpty(token)){
            return null;
        }
        User user = userService.getUserByCookie(token, request, response);
        return user;
========================================================================================================
        if(StringUtils.isEmpty(token)){
            return null;
        }
        User user = (User) redisTemplate.opsForValue().get("user:" + token);
```

### 3、JMeter系统压测

**Windows下：**

- 启动`JMeter.bat`图形化界面

- Options==>Choose Language 设置中文界面

- 新建线程组
  
  - 新建HTTP请求默认值：设置协议，IP及端口
  - 新建HTTP请求：设置访问端口及携带的参数
  - 新建监听器===>聚合报告
  - 执行测试即可
  
  针对复杂的，携带Cookie的请求：
  
  - 设置CSV数据文件
  - 设置HTTP Cookie管理器
  - 执行测试即可

- 查看聚合报告，明确QPS。

**Linux下：**

- 导入Jmeter的tgz包
- Windows下设置好jmx配置文件，导入linux
- 执行`./jmeter.sh  -n -t /opt/first.jmx -l result.jtl`命令
- 生成的result.jtl文件传回Windows客户端下查看。

### 4、注解实现validator登录验证

> 简化代码，使代码更加优雅

**导入依赖**

```xml
        <!--SpringBoot校验组件-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

**简单使用**

- Entity中的参数，按需加上@NotNull，@Length等注解
- Controller层传入的参数，加上@Validator注解

**自定义Validator**

```java
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {IsMobileValidator.class}//校验类
)
public @interface IsMobile {

    boolean required() default true;//要求手机号必须填写

    String message() default "手机号码格式错误";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

```
public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {
    private boolean required = false;

    @Override
    public void initialize(IsMobile constraintAnnotation) {
        required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if(required){//必填情况下
            return ValidatorUtil.isMobile(value);
        }else{//非必填情况下
            if(StringUtils.isEmpty(value)){
                return true;
            }else {
                return ValidatorUtil.isMobile(value);
            }
        }
    }
}
```

### 5、Redis预减库存功能及内存标记

**初始化**

- 实现IntializationBean接口，并重写afterPropertiesSet()方法
- afterPropertiesSet()方法将商品数量放入redis中
- afterPropertiesSet()方法将商品为空的map设置为false

```java
    //实现InitializingBean,实现初始化方法
    @Override
    public void afterPropertiesSet() throws Exception {
        //初始化阶段将所有库存置于redis中
        List<GoodsVo> goodsVoList = goodsService.getGoodsVo();
        if(CollectionUtils.isEmpty(goodsVoList)){
            return;
        }
        goodsVoList.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods" + goodsVo.getId(), goodsVo.getSeckillStock());
            emptyStockMap.put(goodsVo.getId(),false);
        });
    }
```

**秒杀扣减库存阶段**

- 读取内存标记，商品不为空即进入下一步。
- redis扣减库存（可考虑Lua脚本保证原子性）。库存为空时设置内存标记，商品为空设置为true。

```java
        //内存标记，减少和redis的通信
        if(emptyStockMap.get(goodId)){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }

        //递减,获取递减之后的库存
        Long stock = (Long) redisTemplate.execute(
                script, Collections.singletonList("seckillGoods" + goodId), Collections.EMPTY_LIST);
        if(stock <= 0){
            //更改内存标记
            emptyStockMap.put(goodId, true);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
```

### 6、超卖问题的解决

> 原子性操作避免超卖：
> 
> 超卖的原因在于高并发情况下多线程的对数据的重复读写，核心解决方案就是原子性操作。
> 
> 原子性操作的含义就是一个线程在对数据修改的阶段，其他线程不能对数据写入。
> 
> 这一般有乐观锁和悲观锁的解决方案。

在本项目中，主要考虑MySQL的超卖问题和Redis的超卖问题。

- MySQL主要考虑单条SQL语句的原子性。开启事务后，执行单条SQL语句，即具有原子性。
  
  ```java
          //解决超卖问题，库存不允许小于一
          boolean updateRes = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                  .setSql("seckill_stock = seckill_stock - 1")
                  .eq("goods_id", goodsVo.getId())
                  .gt("seckill_stock", 0)
          );
  
          if(!updateRes){
              //未更新成功的情况，设置空库存
              redisTemplate.opsForValue().set("isEmptyStock:" + id,"1");
              return null;
          }
  ```
  
  以上的SQL语句是单条SQL语句，具有原子性。

- Redis考虑Lua脚本的原子性。Lua脚本详见下一小节。

### 7、Redis分布式锁

> Setnx操作以及Lua脚本的简单使用

Setnx本身即具有原子性：

```java
    @Test//基本的setnx
    public void redisTest1(){
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
```

Lua脚本的使用：

- 创建lua脚本如下：stock.lua    ===>   其中KEYS[1]指传入的参数集合中的第一个参数

```lua
if (redis.call('exists',KEYS[1]) == 1) then
    local stock = tonumber(redis.call('get',KEYS[1]));
    if(stock > 0) then
        redis.call('incrby',KEYS[1],-1);
        return stock;
    end;
        return 0;
end;
```

- redis配置类加上lua脚本的配置

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //配置redis的序列化
        //key序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //value序列化
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        //hash序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        //注入连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    //lua脚本
    @Bean
    public DefaultRedisScript<Long> script(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        //设置脚本位置
        script.setLocation(new ClassPathResource("stock.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
```

- 调用lua脚本实现原子性操作，为空时修改内存标记

```java
        //递减,获取递减之后的库存
        Long stock = (Long) redisTemplate.execute(
                script, Collections.singletonList("seckillGoods" + goodId), Collections.EMPTY_LIST);
        if(stock <= 0){
            //更改内存标记
            emptyStockMap.put(goodId, true);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }
```

### 8、SpringBoot整合RabbitMQ

> 快速整合几种交换机模式，以及Sender和Receiver

- 启动RabbitMQ(linux虚拟机 systemctl start || docker run mq)

- SpringBoot工程引入AMQP依赖

```xml
        <!--rabbitMQ依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
```

- 配置application.yml文件

```yml
  # 配置rabbitmq
  rabbitmq:
    host: 192.168.200.130
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        # 消费者最小数量
        concurrency: 10
        # 最大数量
        max-concurrency: 10
        # 每次获取1条消息
        prefetch: 1
        # 默认启动
        auto-startup: true
        # 被拒绝时重新进入队列
        default-requeue-rejected: true
```

- 新建RabbitMQConfig文件，配置queue、交换机，并进行绑定。

```java
    //==============以下为本项目中使用的Topic模式====================
    private static final String seckillQueue = "seckillQueue";
    private static final String seckillExchange = "seckillExchange";

    @Bean
    public Queue seckillQueue(){
        return new Queue(seckillQueue);
    }

    @Bean
    public TopicExchange seckillExchange(){
        return new TopicExchange(seckillExchange);
    }

    @Bean
    public Binding seckillBinding(){
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with("seckill.#");
    }
```

- 创建MQSender，直接将对象convertAndSend

```java
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //以下为本项目中使用的MQSender
    public void seckillSender(Object message){
        rabbitTemplate.convertAndSend("seckillExchange","seckill.msg",message);
    }
}
```

- 创建MQReceiver，绑定RabbitListener，接收指定队列的消息，并进行秒杀操作。

```java
@Service
@Slf4j
public class MQReceiver {
    //==================以下为本项目中使用的Receiver================
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillOrderService seckillOrderService;

    @RabbitListener(queues = "seckillQueue")
    public void receiveSeckillMsg(String message){
        SeckillMsg msg = JsonUtil.jsonStr2Object(message,SeckillMsg.class);
        User user = msg.getUser();
        String goodsId = msg.getGoodsId();
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodsId);
        //再次判断商品库存，因前面判断的是redis中的库存，此时判断的为mysql中的库存
        if(goodsVo.getSeckillStock() < 1){
            return;
        }
        //再次判断是否重复抢购
        Order isBuy = (Order) redisTemplate.opsForValue().get("order" + user.getId() + goodsId);
        if(null != isBuy){
            return;
        }
        //正常下单
        seckillOrderService.saveSecKillOrder(goodsVo, user);
    }
}
```

- Controller层的异步下单，实现流量削峰：此时给前端返回的是排队中，使前端调用`getResult`接口，查询是否下单成功。

```java
        //MQ发送给队列
        SeckillMsg seckillMsg = new SeckillMsg();
        seckillMsg.setUser(user);
        seckillMsg.setGoodsId(goodId);
        mqSender.seckillSender(JsonUtil.object2JsonStr(seckillMsg));
        return RespBean.success(0);//返回排队中
```

### 9、验证码的简单实现

> 一般选用开源的验证码项目即可，本次以gitee一个开源项目为例，其他使用详见该项目的Readme即可。

**导入依赖**

```xml
        <dependency>
            <groupId>com.github.whvcse</groupId>
            <artifactId>easy-captcha</artifactId>
            <version>1.6.2</version>
        </dependency>
```

**后端完成接口**

- 生成验证码
- 验证码放入redis
- 返还验证码的输出流

```java
@RequestMapping("/captcha")
@ResponseBody
public void getCaptcha(User user, String goodId, HttpServletResponse response){
    if(null == user){
        throw new GlobalException(RespBeanEnum.ILLEGAL_REQUEST);
    }
    response.setContentType("image/jpg");
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache");//关闭缓存
    response.setDateHeader("Expires", 0);//永不失效
    ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 30, 3);
    //验证码放入redis，并设置失效时间
    redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodId, captcha.text(),5, TimeUnit.MINUTES);
    try {
        captcha.out(response.getOutputStream());
    } catch (IOException e) {
        log.error("验证码生成失败!");
        e.printStackTrace();
    }
}
```

**前端调用接口**

验证码的接口返回的便是OutputStream对象，可供前端直接读取。

```html
<img id="captcha" width="130px" height="32px" onclick="refreshCaptcha()"/>
===============
function refreshCaptcha(){
    $("#captcha").attr("src","/seckill/captcha?goodId=" + $("#goodsId").val() + "&time=" + new Date())
};
```

### 10、秒杀地址的隐藏

> 避免暴露真实接口位置，实现接口防刷

**实现原理**

- 前端将秒杀按钮直接对应的地址改为`getPath`，获取访问路径
- `getPath`中基于UUID和MD5生成str字符串，并存入redis，同时将str返还为前端
- 前端拿到str串，调用doSecKill()方法，访问`/str/doSecKill`接口，传入User，商品ID。
- 后端根据str、User、商品ID，查询redis，若数据吻合，则进行下一步的真实秒杀代码。

**代码实现**

前端：

```js
function getSeckillPath(){
    var goodId = g_getQueryString("goodId");
    var captchaText = $("#confirmCaptcha").val();
    console.log(captchaText)
    g_showLoading();
    $.ajax({
        url:'seckill/getPath',
        type:'POST',
        data:{
            goodId:goodId,
            captchaText:captchaText
        },
        success:function (data){
            if(data.code == 200){
                var path = data.obj;
                doSeckill(path);
            }else if(data.code == 50021){
                layer.msg(data.message)
            } else if(data.code == 50022){
                layer.msg(data.message);
            }
        },
        error:function (){
            layer.msg("客户端请求错误");
        }
    })
}
```

```js
function doSeckill(path){
    var goodId = $("#goodsId").val()
    console.log(goodId)
    $.ajax({
        url:'/seckill/' + path + '/doSeckill2/' + goodId,
        type: 'POST',
        success:function (data){
            if(data.code == 200){
                getResult(goodId);
            }else {
                layer.msg(data.message);
            }
        },
        error:function (){
            layer.msg("客户端请求失败")
        }
    })
}
```

后端：

```java
    //获取真实秒杀地址
    @RequestMapping("/getPath")
    @ResponseBody
    @AccessLimit(second = 5,maxCount = 5,needLogin = true)//注解实现对接口的访问限制判断
    public RespBean getPath(User user, String goodId, String captchaText, HttpServletRequest request){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //校验验证码
        String redisCaptcha = (String) valueOperations.get("captcha:" + user.getId() + ":" + goodId);
        if(!captchaText.equals(redisCaptcha)){
            return RespBean.error(RespBeanEnum.WRONG_CAPTCHA);
        }
        String str = orderService.createPath(user,goodId);
        return RespBean.success(str);
    }
==========================================================================================
    @PostMapping("/{path}/doSeckill2/{goodId}")
    @ResponseBody
    public RespBean doSecKill2(User user, @PathVariable("goodId") String goodId, @PathVariable("path") String path) {
        if (null == user) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //判断请求是否在redis中
        boolean isPath = orderService.checkPath(user, goodId, path);
        if(!isPath){
            return RespBean.error(RespBeanEnum.ILLEGAL_REQUEST);
        }
 ==========================================================================================
    @Override
    public String createPath(User user, String goodId) {
        String path = MD5Util.md5(UUIDUtil.uuid() + "123456");//生成接口地址
        //存入数据库，设置超时时间60s
        redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodId, path,60, TimeUnit.SECONDS);
        return path;
    }

    //校验秒杀地址
    @Override
    public boolean checkPath(User user, String goodId, String path) {
        if(null == user || StringUtils.isEmpty(path)){
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodId);
        if(path.equals(redisPath)){
            return true;
        }
        return false;
    }
```

### 11、接口限流功能的实现

> 实现简单的计数接口限流

实现原理：访问次数存入redis，初始化为1，设置有效时间5秒；每次访问 + 1，>5则报错。

```java
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //校验单位时间的请求次数，限制访问次数，5秒内访问5次
        String uri = request.getRequestURI();
        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
        if(null == count){
            valueOperations.set(uri + ":" + user.getId(), 1,5,TimeUnit.SECONDS);
        }else if(count < 5){
            valueOperations.increment(uri + ":" + user.getId());
        }else {
            return RespBean.error(RespBeanEnum.REPEAT_REQUEST);
        }
```

计数接口限流是一种简单的限流实现，但实际更广泛采用的是令牌桶策略，本项目暂未使用。

### 12、ThreadLocal实现用户信息存储

**实现原理**

- 并发编程中重要的问题就是数据共享，当在一个线程中改变任意属性时，所有的线程都会因此受到影响，同时会看到第一个线程修改后的值。
- 在有些情况下，需要实现单个线程独享自身的数据，此时ThreadLocal可以实现。
- 本项目中，各个线程均有自己的User对象，采用ThreadLocal存储User即可。

**实现过程**

- 创建UserContext类，设置User的get和set方法

```java
public class UserContext {

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user){
        userHolder.set(user);
    }

    public static User getUser(){
        return userHolder.get();
    }
}
```

- 用户首次登录时，拦截器从request中取出token，并利用token从redis中取出User，存入UserContext的ThreadLocal中

```java
    User user = getUser(request,response);
    UserContext.setUser(user);
========================================================================================
    //获取用户对象
    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtil.getCookieValue(request, "userToken");
        if(StringUtils.isEmpty(token)){
            return null;
        }
        return userService.getUserByCookie(token, request, response);
    }           
```

### 13、AccessLimit注解的实现

> 简化代码，使代码更加优雅

本注解用于Controller层，主要用于进行接口限流。本注解通过Interceptor实现。

- 建立注解AccessLimit

```java
@Retention(RetentionPolicy.RUNTIME)//运行时
@Target(ElementType.METHOD)//设置为方法的注解
public @interface AccessLimit {
    int second() default 5;
    int maxCount() default 5;
    boolean needLogin();
}
```

- Inteceptor实现注解的功能

```java
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            User user = getUser(request,response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod)handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            //判断是否带注解，有该注解则开启拦截
            if(null == accessLimit) {
                return true;
            }
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            String uri = request.getRequestURI();
            if(needLogin){
                //判断用户登录
                if(null == user){
                    render(response, RespBeanEnum.SESSION_ERROR);
                    return false;
                }
                //判断用户是否重复刷新，过于频繁
                String key = uri + ":" + user.getId();
                ValueOperations valueOperations = redisTemplate.opsForValue();
                Integer count = (Integer) valueOperations.get(key);
                if(null == count){
                    valueOperations.set(uri + ":" + user.getId(), 1,maxCount, TimeUnit.SECONDS);
                }else if(count < second){
                    valueOperations.increment(uri + ":" + user.getId());
                }else {
                    render(response, RespBeanEnum.REPEAT_REQUEST);
                    return false;
                }
            }
        }
        return true;
    }

    //构建返还对象
    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        writer.write(new ObjectMapper().writeValueAsString(respBean));
        writer.flush();
        writer.close();
    }

    //获取用户对象
    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtil.getCookieValue(request, "userToken");
        if(StringUtils.isEmpty(token)){
            return null;
        }
        return userService.getUserByCookie(token, request, response);
    }
}
```

## 三、项目总结

**项目框架搭建**

- SpringBoot集成开发环境
- 集成Thymeleaf模板
- 搭建RespBean和全局异常处理机制

**分布式会话**

- 用户登录及密码加密
- 共享Session：redis + SpringSession

**功能开发**

- 商品列表
- 商品详情
- 秒杀页面
- 订单详情

**系统压测**

- JMeter的基础使用
- 自定义变量模拟多用户

**页面优化**

- 页面缓存：redis实现
- 对象缓存：redis实现
- 页面静态化：前后端分离

**接口优化**

- redis预减库存
- JVM内存标记
- RabbitMQ异步下单
- redis分布式锁

**安全优化**

- 隐藏秒杀接口
- 验证码
- 接口限流
