//package com.cn.tianxia.api.base.mq;
//
//import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.beans.factory.config.ConfigurableBeanFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Scope;
//
///**
// * 
// * @ClassName RabbitConfig
// * @Description rabbitmq配置类
// * @author Hardy
// * @Date 2019年5月19日 下午12:06:19
// * @version 1.0.0
// */
//@Configuration
//public class MQConfig {
//
//    @Value("${spring.rabbitmq.host}")
//    private String host;
//    @Value("${spring.rabbitmq.port}")
//    private Integer port;
//    @Value("${spring.rabbitmq.username}")
//    private String username;
//    @Value("${spring.rabbitmq.password}")
//    private String password;
//    @Value("${spring.rabbitmq.virtual-host}")
//    private String virtualHost;
//    @Value("${spring.rabbitmq.publisher-confirms}")
//    private boolean publisherConfirms;
//    @Value("${spring.rabbitmq.publisher-returns}")
//    private boolean publisherReturns;
//    
//    
//    /**
//     * 
//     * @Description mq链接配置
//     * @return
//     */
//    @Bean
//    public ConnectionFactory connectionFactory(){
//        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(this.host,this.port);
//        connectionFactory.setUsername(this.username);
//        connectionFactory.setPassword(this.password);
//        connectionFactory.setVirtualHost(this.virtualHost);
//        connectionFactory.setPublisherConfirms(this.publisherConfirms);
//        connectionFactory.setPublisherReturns(this.publisherReturns);
//        return connectionFactory;
//    }
//    
//    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//    public RabbitTemplate rabbitTemplate(){
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
//        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
//        return rabbitTemplate;
//    }
//}
