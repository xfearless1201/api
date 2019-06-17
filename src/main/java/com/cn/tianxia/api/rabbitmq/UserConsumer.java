//package com.cn.tianxia.api.rabbitmq;
//
//import java.io.IOException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitHandler;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import com.alibaba.fastjson.JSONObject;
//import com.cn.tianxia.api.base.consts.MQTopicConsts;
//import com.cn.tianxia.api.project.v2.OnlineUserEntity;
//import com.cn.tianxia.api.service.v2.OnlineUserService;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Consumer;
//import com.rabbitmq.client.DefaultConsumer;
//
//@Component
//public class UserConsumer{
//
//    private static final Logger logger = LoggerFactory.getLogger(UserConsumer.class);
//    
//    @Autowired
//    private OnlineUserService onlineUserService;
//    
//    
//    /**
//     * 
//     * @Description 处理在线会员
//     * @param message
//     * @param channel
//     * @throws IOException
//     */
//    @RabbitListener(queues = MQTopicConsts.TOPIC_QUEUE_ONLINE)
//    @RabbitHandler
//    public void processOnlineUser(Message message,Channel channel){
//        logger.info("消费MQ信息写入用户在线状态信息开始================START========================");
//        try {
//            long deliveryTag = message.getMessageProperties().getDeliveryTag();
//            Consumer consumer = new DefaultConsumer(channel);
//            channel.basicConsume(MQTopicConsts.TOPIC_QUEUE_ONLINE, false, consumer);
//            OnlineUserEntity online = JSONObject.parseObject(message.getBody(), OnlineUserEntity.class);
//            //通过用户ID查询是否存在
//            OnlineUserEntity onlineUser = onlineUserService.getByUid(String.valueOf(online.getUid()));
//            if(onlineUser == null){
//                //首次登录 或 注册,只能是上线操作
//                BeanUtils.copyProperties(onlineUser, online);
//            }else{
//                onlineUser.setLogoutTime(System.currentTimeMillis());
//                onlineUser.setOffStatus((byte)0);
//                onlineUser.setUid(online.getUid());
//                if(String.valueOf(online.getIsOff()).equals("0")){
//                    //会员登录操作
//                    onlineUser.setIsOff((byte)0);
//                }else{
//                    //离线操作
//                    onlineUser.setIsOff((byte)1);
//                }
//            }
//            onlineUserService.insertOrUpdateOnlineUser(onlineUser);
//            channel.basicAck(deliveryTag, false);
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.info("MQ写入在线会员信息异常:{}",e.getMessage());
//        }
//    }
//    
//    @RabbitListener(queues = MQTopicConsts.TOPIC_QUEUE_DEMO1)
//    @RabbitHandler
//    public void processDemo(Message message){
//        logger.info("接收到的MQ消息是:{}",message);
//    }
//    
//}
