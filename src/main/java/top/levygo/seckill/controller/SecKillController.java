package top.levygo.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.levygo.seckill.config.AccessLimit;
import top.levygo.seckill.entity.Order;
import top.levygo.seckill.entity.SeckillMsg;
import top.levygo.seckill.entity.SeckillOrder;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.exception.GlobalException;
import top.levygo.seckill.rabbitmq.MQSender;
import top.levygo.seckill.service.GoodsService;
import top.levygo.seckill.service.SeckillOrderService;
import top.levygo.seckill.entity.vo.GoodsVo;
import top.levygo.seckill.entity.vo.RespBean;
import top.levygo.seckill.entity.vo.RespBeanEnum;
import top.levygo.seckill.utils.JsonUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description：秒杀入口
 * @author：LevyXie
 * @create：2022-04-12 15:52
 */
@Controller
@RequestMapping("/seckill")
@Slf4j
public class SecKillController implements InitializingBean {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private SeckillOrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisScript script;

    @Autowired
    private MQSender mqSender;

    private Map<String,Boolean> emptyStockMap = new HashMap<>();//将商品信息存入内存中，减少和redis的通信

    //未修改为静态页面之前的秒杀方法
    @PostMapping("/doSeckill")
    public String doSecKill(Model model, User user,String goodId){
        if(null == user){
            return "login";
        }
        model.addAttribute("user",user);
        //判断库存
        GoodsVo goodsVo = goodsService.getGoodsVoById(goodId);
        if(goodsVo.getSeckillStock() < 1){
            model.addAttribute("errMsg", RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR).getMessage());
            return "secKillFail";
        }
        //判断是否重复抢购
        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        wrapper.eq("goods_id", goodId);
        SeckillOrder isBuy = orderService.getOne(wrapper);
        if(null != isBuy){
            model.addAttribute("errMsg", RespBean.error(RespBeanEnum.ALREADY_BUY_ERROR).getMessage());
            return "secKillFail";
        }
        Order order = orderService.saveSecKillOrder(goodsVo,user);
        model.addAttribute("order",order);
        model.addAttribute("goods",goodsVo);
        return "orderDetail";
    }

    //修改为静态页面，并采用redis&mq优化后的方法
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
//        //判断库存
//        GoodsVo goodsVo = goodsService.getGoodsVoById(goodId);
//        if(goodsVo.getSeckillStock() < 1){
//            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
//        }
//        //判断是否重复抢购
////        QueryWrapper<SeckillOrder> wrapper = new QueryWrapper<>();
////        wrapper.eq("user_id", user.getId());
////        wrapper.eq("goods_id", goodId);
////        SeckillOrder isBuy = orderService.getOne(wrapper);
//        //改为从redis中取数据，判断是否已经抢购
//        Order isBuy = (Order) redisTemplate.opsForValue().get("order" + user.getId() + goodId);
//        if(null != isBuy){
//            return RespBean.error(RespBeanEnum.ALREADY_BUY_ERROR);
//        }
//
//        Order order = orderService.saveSecKillOrder(goodsVo,user);
//        return RespBean.success(order);

        //以下为利用redis预减库存
        //从redis中取数据，判断是否已经抢购
        Object isBuy = valueOperations.get("order:" + user.getId() + goodId);
        if(null != isBuy){
            return RespBean.error(RespBeanEnum.ALREADY_BUY_ERROR);
        }

        //内存标记，减少和redis的通信
        if(emptyStockMap.get(goodId)){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }

        //递减,获取递减之后的库存
//        Long stock = valueOperations.decrement("seckillGoods" + goodId);
        Long stock = (Long) redisTemplate.execute(
                script, Collections.singletonList("seckillGoods" + goodId), Collections.EMPTY_LIST);
        if(stock <= 0){
            //更改内存标记
            emptyStockMap.put(goodId, true);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK_ERROR);
        }

        //MQ发送给队列
        SeckillMsg seckillMsg = new SeckillMsg();
        seckillMsg.setUser(user);
        seckillMsg.setGoodsId(goodId);
        mqSender.seckillSender(JsonUtil.object2JsonStr(seckillMsg));
        return RespBean.success(0);//返回排队中
    }

    //获取是否秒杀成功的结果
    @PostMapping("/getKillResult")
    @ResponseBody
    public RespBean getKillResult(User user,String goodId){
        if(null == user){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        String orderId = orderService.getKillResult(user,goodId);
        return RespBean.success(orderId);
    }

    //获取真实秒杀地址
    @RequestMapping("/getPath")
    @ResponseBody
    @AccessLimit(second = 5,maxCount = 5,needLogin = true)//注解实现对接口的访问限制判断
    public RespBean getPath(User user, String goodId, String captchaText, HttpServletRequest request){
//        if(null == user){
//            return RespBean.error(RespBeanEnum.SESSION_ERROR);
//        }
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        //校验单位时间的请求次数，限制访问次数，5秒内访问5次
//        String uri = request.getRequestURI();
//        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
//        if(null == count){
//            valueOperations.set(uri + ":" + user.getId(), 1,5,TimeUnit.SECONDS);
//        }else if(count < 5){
//            valueOperations.increment(uri + ":" + user.getId());
//        }else {
//            return RespBean.error(RespBeanEnum.REPEAT_REQUEST);
//        }
        //以上功能已被注解实现
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //校验验证码
        String redisCaptcha = (String) valueOperations.get("captcha:" + user.getId() + ":" + goodId);
        if(!captchaText.equals(redisCaptcha)){
            return RespBean.error(RespBeanEnum.WRONG_CAPTCHA);
        }
        String str = orderService.createPath(user,goodId);
        return RespBean.success(str);
    }

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
}
