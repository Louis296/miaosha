package com.louis296.miaoshaweb.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue delCacheQueue(){
        return new Queue("delCache");
    }
}
