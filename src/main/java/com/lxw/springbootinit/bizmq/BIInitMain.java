package com.lxw.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于创建测试程序用到的交换机和队列(只用在程序启动前执行一次)
 */
public class BIInitMain {

    public static void main(String[] argv){
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("1.15.229.51");
            factory.setUsername("admin");
            factory.setPassword("123456");
            factory.setAutomaticRecoveryEnabled(true); // 开启自动重连
            factory.setNetworkRecoveryInterval(5000); // 重连间隔 5 秒
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            //声明死信交换机和死信队列
            channel.exchangeDeclare(BiMqConstant.BI_DEAD_EXCHANGE_NAME,"direct",true);
            channel.queueDeclare(BiMqConstant.BI_DEAD_QUEUE_NAME,true,false,false,null);
            channel.queueBind(BiMqConstant.BI_DEAD_QUEUE_NAME,BiMqConstant.BI_DEAD_EXCHANGE_NAME,BiMqConstant.BI_DEAD_ROUTING_KEY);
            //指定死信队列参数
            Map<String, Object> args = new HashMap<>();
            //要绑定哪个死信交换机
            args.put("x-dead-letter-exchange", BiMqConstant.BI_DEAD_EXCHANGE_NAME);
            //指定死信要转发到哪个死信队列
            args.put("x-dead-letter-routing-key", BiMqConstant.BI_DEAD_ROUTING_KEY);
            //声明工作交换机和队列
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME, "direct",true);
            channel.queueDeclare(BiMqConstant.BI_QUEUE_NAME, true, false, false, args);
            channel.queueBind(BiMqConstant.BI_QUEUE_NAME, BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}