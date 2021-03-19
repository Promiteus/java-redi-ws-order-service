package com.romanm.websocket_and_redis.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.JsonConverter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisProducer {
    private StringRedisTemplate redisTemplate;
    private RusCharsTranslator rusCharsTranslator;
    private Topic<Order> topic;
    private JsonConverter<Order> jsonConverter;

    @Autowired
    public RedisProducer(StringRedisTemplate  redisTemplate,
                         RusCharsTranslator rusCharsTranslator,
                         Topic<Order> topic,
                         JsonConverter<Order> jsonConverter) {
        this.redisTemplate = redisTemplate;
        this.rusCharsTranslator = rusCharsTranslator;
        this.topic = topic;
        this.jsonConverter = jsonConverter;
    }


    public void newOrderNotify(@NonNull Order order) {
        log.info(String.format("Producer sent order to %s!", topic.getTopic(order)));
        try {
            this.redisTemplate.convertAndSend(topic.getTopic(order), jsonConverter.convertObjectToJson(order));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
