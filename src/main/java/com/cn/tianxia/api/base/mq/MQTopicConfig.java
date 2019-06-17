//package com.cn.tianxia.api.base.mq;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.TopicExchange;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.cn.tianxia.api.base.consts.MQTopicConsts;
//
///**
// * 
// * @ClassName MQTopicConfig
// * @Description topic模式
// * @author Hardy
// * @Date 2019年5月19日 下午10:26:37
// * @version 1.0.0
// */
//@Configuration
//public class MQTopicConfig {
//
//    /**
//     * 
//     * @Description 登录队列
//     * @return
//     */
//    @Bean
//    Queue queueOnline(){
//        return new Queue(MQTopicConsts.TOPIC_QUEUE_ONLINE,true);
//    }
//    
//    
//    @Bean
//    Queue queueDemo1(){
//        return new Queue(MQTopicConsts.TOPIC_QUEUE_DEMO1,true);
//    }
//    
//    @Bean
//    Queue queueDemo2(){
//        return new Queue(MQTopicConsts.TOPIC_QUEUE_DEMO2,true);
//    }
//    
//    
//    
//    
//    /**
//     * 
//     * @Description topic交换机
//     * @return
//     */
//    @Bean
//    TopicExchange topicExchange(){
//        return new TopicExchange(MQTopicConsts.TOPIC_EXCHANGE);
//    }
//    
//    
//    @Bean
//    Binding bindingExchangeOnline(@Qualifier("queueOnline") Queue queueOnline,TopicExchange topicExchange){
//        return BindingBuilder.bind(queueOnline).to(topicExchange).with(MQTopicConsts.TOPIC_ONLINE_KEY);
//    }
//    
//    
//    @Bean
//    Binding bindingExchangeDemo1(Queue queueDemo1,TopicExchange topicExchange){
//        return BindingBuilder.bind(queueDemo1).to(topicExchange).with(MQTopicConsts.TOPIC_MORE_KEY);
//    }
//    
//    @Bean
//    Binding bindingExchangeDemo2(Queue queueDemo2,TopicExchange topicExchange){
//        return BindingBuilder.bind(queueDemo2).to(topicExchange).with(MQTopicConsts.TOPIC_MORE_KEY);
//    }
//}
