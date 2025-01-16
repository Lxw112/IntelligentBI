package com.lxw.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DlxDirectProducer {

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("192.168.182.128");
      factory.setUsername("itheima");
      factory.setPassword("123321");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        //声明死信交换机和死信队列
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");
        String queueName = "laoban_dlx_queue";
        channel.queueDeclare(queueName,true,false,false,null);
        channel.queueBind(queueName, DEAD_EXCHANGE_NAME, "laoban");

        String queueName2 = "waibao_dlx_queue";
        channel.queueDeclare(queueName2,true,false,false,null);
        channel.queueBind(queueName2, DEAD_EXCHANGE_NAME, "waibao");

        DeliverCallback laoban_deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            System.out.println(" [laoban] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
        };
        DeliverCallback waibao_deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [waibao] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
        };
        channel.basicConsume(queueName, false, laoban_deliverCallback1, consumerTag -> { });
        channel.basicConsume(queueName2, false, waibao_deliverCallback2, consumerTag -> { });

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String userInput = scanner.nextLine();
            String[] strings = userInput.split(" ");
            if (strings.length < 1){
                continue;
            }
            String message = strings[0];
            String routingKey =strings[1];
            channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        }
    }
  }
  //..
}