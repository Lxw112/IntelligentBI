package com.lxw.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试程序用到的交换机和队列(只用在程序启动前执行一次)
 */
public class BIInitMain {

    public static void main(String[] argv){
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.182.128");
            factory.setUsername("itheima");
            factory.setPassword("123321");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME, "direct");
            channel.queueDeclare(BiMqConstant.BI_QUEUE_NAME, true, false, false, null);
            channel.queueBind(BiMqConstant.BI_QUEUE_NAME, BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}