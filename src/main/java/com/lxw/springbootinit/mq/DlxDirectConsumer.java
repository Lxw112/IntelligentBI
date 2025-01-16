package com.lxw.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {

  private static final String WORK_EXCHANGE_NAME = "direct2_exchange";
  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("192.168.182.128");
      factory.setUsername("itheima");
      factory.setPassword("123321");
    Connection connection = factory.newConnection();
    Channel channel1 = connection.createChannel();
    Channel channel2 = connection.createChannel();

    channel1.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");
    //指定死信队列参数
    Map<String, Object> args = new HashMap<>();
    //要绑定哪个死信交换机
    args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
    //指定死信要转发到哪个死信队列
    args.put("x-dead-letter-routing-key", "waibao");

    String queueName1 = "xiaodog_queue";
    channel1.queueDeclare(queueName1,true,false,false,args);
    channel1.queueBind(queueName1, WORK_EXCHANGE_NAME, "xiaodog");

    Map<String, Object> args2 = new HashMap<>();
    args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
    args2.put("x-dead-letter-routing-key", "laoban");

      String queueName2 = "xiaocat_queue";
      channel2.queueDeclare(queueName2,true,false,false,args2);
      channel2.queueBind(queueName2, WORK_EXCHANGE_NAME, "xiaocat");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        //拒绝消息
        channel1.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
        System.out.println(" [xiaodog] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
        channel2.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
          System.out.println(" [xiaocat] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
    channel1.basicConsume(queueName1, false, deliverCallback1, consumerTag -> { });
    channel2.basicConsume(queueName2, false, deliverCallback2, consumerTag -> { });
  }
}