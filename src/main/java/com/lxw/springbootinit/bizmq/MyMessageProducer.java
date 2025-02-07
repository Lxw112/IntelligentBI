package com.lxw.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Profile("dev")  // 仅在dev环境下加载该类
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;
    public void sendMessage(String exchange,String routingKey,String message){
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
    }
}
