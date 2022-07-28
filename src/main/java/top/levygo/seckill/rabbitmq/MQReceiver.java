package top.levygo.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.levygo.seckill.entity.Order;
import top.levygo.seckill.entity.SeckillMsg;
import top.levygo.seckill.entity.User;
import top.levygo.seckill.entity.vo.GoodsVo;
import top.levygo.seckill.entity.vo.RespBean;
import top.levygo.seckill.entity.vo.RespBeanEnum;
import top.levygo.seckill.service.GoodsService;
import top.levygo.seckill.service.SeckillOrderService;
import top.levygo.seckill.utils.JsonUtil;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-14 9:11
 */
@Service
@Slf4j
public class MQReceiver {

    @RabbitListener(queues = "queue")
    public void receive(Object obj){
        log.info("接收" + obj);
    }

    @RabbitListener(queues = "queue_fanout01")
    public void receiveFanOut1(Object obj){
        log.info("Fanout_QUEUE01接收" + obj);
    }

    @RabbitListener(queues = "queue_fanout02")
    public void receiveFanOut2(Object obj){
        log.info("Fanout_QUEUE02接收" + obj);
    }

    @RabbitListener(queues = "queue_direct01")
    public void receiveDirect1(Object obj){
        log.info("Direct_QUEUE01接收" + obj);
    }

    @RabbitListener(queues = "queue_direct02")
    public void receiveDirect2(Object obj){
        log.info("Direct_QUEUE02接收" + obj);
    }

    @RabbitListener(queues = "queue_topic01")
    public void receiveTopic1(Object obj){
        log.info("Topic_QUEUE01接收" + obj);
    }

    @RabbitListener(queues = "queue_topic02")
    public void receiveTopic2(Object obj){
        log.info("Topic_QUEUE02接收" + obj);
    }

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
