package top.levygo.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description：RabbitMQ配置类
 * @author：LevyXie
 * @create：2022-04-14 9:02
 */
@Configuration
public class RabbiMQConfig {

    private static final String QUEUE01 =  "queue_fanout01";
    private static final String QUEUE02 =  "queue_fanout02";
    private static final String EXCHANGE=  "fanoutExchange";

    private static final String QUEUED01 = "queue_direct01";
    private static final String QUEUED02 = "queue_direct02";
    private static final String DIRECT_EXCHANGE = "directExchange";
    private static final String ROUTING_KEY01 = "queue.red";
    private static final String ROUTING_KEY02 = "queue.blue";

    private static final String QUEUET01 = "queue_topic01";
    private static final String QUEUET02 = "queue_topic02";
    private static final String TOPIC_EXCHANGE = "topicExchange";
    private static final String TOPIC_KEY01 = "#.queue.#";
    private static final String TOPIC_KEY02 = "*.queue.#";

    @Bean
    public Queue queue(){
        return new Queue("queue",true);//配置持久化
    }


    //==============以下为fanoutExchange的测试====================
    @Bean
    public Queue queue01(){
        return new Queue(QUEUE01);
    }
    @Bean
    public Queue queue02(){
        return new Queue(QUEUE02);
    }

    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(EXCHANGE);
    }

    @Bean
    public Binding binding01(){
        return BindingBuilder.bind(queue01()).to(fanoutExchange());
    }

    @Bean
    public Binding binding02(){
        return BindingBuilder.bind(queue02()).to(fanoutExchange());
    }

    //==============以下为directExchange的测试====================
    @Bean
    public Queue queueD01(){
        return new Queue(QUEUED01);
    }

    @Bean
    public Queue queueD02(){
        return new Queue(QUEUED02);
    }

    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange(DIRECT_EXCHANGE);
    }

    @Bean
    public Binding bindingD01(){
        return BindingBuilder.bind(queueD01()).to(directExchange()).with(ROUTING_KEY01);
    }

    @Bean
    public Binding bindingD02(){
        return BindingBuilder.bind(queueD02()).to(directExchange()).with(ROUTING_KEY02);
    }

    //==============以下为topicExchange的测试====================
    @Bean
    public Queue queueT01(){
        return new Queue(QUEUET01);
    }

    @Bean
    public Queue queueT02(){
        return new Queue(QUEUET02);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public Binding bindingT01(){
        return BindingBuilder.bind(queueT01()).to(topicExchange()).with(TOPIC_KEY01);
    }

    @Bean
    public Binding bindingT02(){
        return BindingBuilder.bind(queueT02()).to(topicExchange()).with(TOPIC_KEY02);
    }

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

}
