//package com.cn.tianxia.api.mq;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import com.cn.tianxia.api.Bootstrap;
//import com.cn.tianxia.api.rabbitmq.UserProducer;
//
///**
// * @ClassName ProducerTest
// * @Description 生产者测试类
// * @author Hardy
// * @Date 2019年5月19日 下午12:17:04
// * @version 1.0.0
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(classes = Bootstrap.class)
//public class ProducerTest {
//
//    @Autowired
//    private UserProducer userProducer;
//
//    @Test
//    public void sendUserLoginInfo() {
//        userProducer.processDemo("测试MQ消息队列的一条狗");
//    }
//
//}
