package com.lxw.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 用于创建测试程序用到的交换机和队列(只用在程序启动前执行一次)
 */
public class MqInitMain {
    private static final String EXCHANGE_NAME = "code_exchange";

    public static void main(String[] argv){
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.182.128");
            factory.setUsername("itheima");
            factory.setPassword("123321");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            String queueName = "code_queue";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}