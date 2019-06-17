//package com.cn.tianxia.api.rabbitmq;
//
//import java.util.UUID;
//
//import org.springframework.amqp.rabbit.connection.CorrelationData;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Component;
//
//import com.cn.tianxia.api.base.consts.MQTopicConsts;
//import com.cn.tianxia.api.project.v2.OnlineUserEntity;
//
//
///**
// * 
// * @ClassName LoginProducer
// * @Description 登录生成者
// * @author Hardy
// * @Date 2019年5月20日 下午12:02:28
// * @version 1.0.0
// */
//@Component
//public class UserProducer{
//
//    /**
//     * 由于rabbitTemplate的scope属性设置为ConfigurableBeanFactory.SCOPE_PROTOTYPE，所以不能自动注入
//     */
//    private RabbitTemplate rabbitTemplate;
//
//    public UserProducer(RabbitTemplate rabbitTemplate) {
//        this.rabbitTemplate = rabbitTemplate;
//    }
//    
//    /**
//     * 
//     * @Description 处理在线会员
//     * @param correlationData
//     * @param user
//     */
//    public void processOnlineUser(OnlineUserEntity online){
//        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
//        rabbitTemplate.convertAndSend(MQTopicConsts.TOPIC_EXCHANGE,MQTopicConsts.TOPIC_ONLINE_KEY,online,correlationData);
//    }
//
//    
//    /**
//     * 
//     * @Description 测试
//     * @param message
//     */
//    public void processDemo(String message){
//        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
//        rabbitTemplate.convertAndSend(MQTopicConsts.TOPIC_EXCHANGE,MQTopicConsts.TOPIC_MORE_KEY,message,correlationData);
//    }
//}
