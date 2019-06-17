package com.cn.tianxia.api.base.consts;

/**
 * 
 * @ClassName MQFanoutConsts
 * @Description MQ的广播或订阅与发布模式的常量类
 * @author Hardy
 * @Date 2019年5月19日 下午6:52:20
 * @version 1.0.0
 */
public class MQTopicConsts {
    
    //测试使用
    public static final String TOPIC_QUEUE_DEMO1="topic.demo1";
    public static final String TOPIC_QUEUE_DEMO2="topic.demo2";
    
    
    //转账fanout
    public static final String TOPIC_QUEUE_TRANSFER="topic.transfer";
    //在线用户信息
    public static final String TOPIC_QUEUE_ONLINE = "topic.online";
    //fanout交换机
    public static final String TOPIC_EXCHANGE="topicExchange";
    
    public static final String TOPIC_ONLINE_KEY="topicKey.online";
    
    public static final String TOPIC_SIMPLE_KEY="topicSimpleKey.*";
    
    public static final String TOPIC_MORE_KEY="topicMoreKey.#";
}
