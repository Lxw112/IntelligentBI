package com.lxw.springbootinit.mq;

import com.rabbitmq.client.*;

public class DirectConsumer {

  private static final String EXCHANGE_NAME = "direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("192.168.182.128");
      factory.setUsername("itheima");
      factory.setPassword("123321");
    Connection connection = factory.newConnection();
    Channel channel1 = connection.createChannel();
    Channel channel2 = connection.createChannel();

    channel1.exchangeDeclare(EXCHANGE_NAME, "direct");
    String queueName1 = "xiaoyu_queue";
    channel1.queueDeclare(queueName1,true,false,false,null);
    channel1.queueBind(queueName1, EXCHANGE_NAME, "xiaoyu");

      String queueName2 = "xiaopi_queue";
      channel2.queueDeclare(queueName2,true,false,false,null);
      channel2.queueBind(queueName2, EXCHANGE_NAME, "xiaopi");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [xiaoyu] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [xiaopi] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
    channel1.basicConsume(queueName1, true, deliverCallback1, consumerTag -> { });
    channel2.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });
  }
}