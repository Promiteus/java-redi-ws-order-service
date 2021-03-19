package com.romanm.websocket_and_redis;

import com.romanm.websocket_and_redis.components.RedisProducer;
import com.romanm.websocket_and_redis.components.RusCharsTranslator;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.services.topics.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.UnsupportedEncodingException;


@Slf4j
@EnableScheduling
@SpringBootApplication
public class WebsocketAndRedisApplication {


    public static void main(String[] args) {
        SpringApplication.run(WebsocketAndRedisApplication.class, args);


    }


}
