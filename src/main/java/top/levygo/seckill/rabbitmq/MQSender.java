package top.levygo.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.One;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.levygo.seckill.entity.SeckillMsg;

/**
 * @description：
 * @author：LevyXie
 * @create：2022-04-14 9:09
 */
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendToWorkQueue(Object obj){
        rabbitTemplate.convertAndSend("queue",obj);
    }

    public void sendToFanOutExchange(Object obj){
        rabbitTemplate.convertAndSend("fanoutExchange","", obj);
    }

    public void sendToDirectExchange01(Object obj){
        rabbitTemplate.convertAndSend("directExchange","queue.red",obj);
    }

    public void sendToDirectExchange02(Object obj){
        rabbitTemplate.convertAndSend("directExchange","queue.blue",obj);
    }

    public void sendToTopicExchange01(Object obj){
        rabbitTemplate.convertAndSend("topicExchange","queue.hello",obj);
    }

    public void sendToTopicExchange02(Object obj){
        rabbitTemplate.convertAndSend("topicExchange","two.queue.hello",obj);
    }

    //以下为本项目中使用的MQSender
    public void seckillSender(Object message){
        rabbitTemplate.convertAndSend("seckillExchange","seckill.msg",message);
    }

}
