package com.lxw.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
@Profile("dev")  // 仅在dev环境下加载该类
public class MyMessageConsumer {

    //指定程序监听的消息队列和确认机制
    @RabbitListener(queues = {"code_queue"},ackMode = "MANUAL")
    @SneakyThrows
    public void receiveMessage(String message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("receiveMessage message = {}",message);
        channel.basicAck(deliveryTag,false);
    }
}
