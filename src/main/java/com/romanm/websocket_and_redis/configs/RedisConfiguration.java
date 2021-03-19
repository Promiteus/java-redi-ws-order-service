package com.romanm.websocket_and_redis.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfiguration {
   /* @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, List.of(
                new PatternTopic("topic.*")
        ));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(Consumer consumer) {
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(consumer);
        return listenerAdapter;
    }*/


    @Bean
    StringRedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }



}
