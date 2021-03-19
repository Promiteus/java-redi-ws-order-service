package com.romanm.websocket_and_redis.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.utils.JsonConverter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RedisOrderConsumer implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] bytes) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("RedisOrderConsumer Message received: "+ json);

        String str = new String(bytes, StandardCharsets.UTF_8);
        log.info("RedisOrderConsumer have got topic pattern: "+str);

        try {
            Order order = new OrderJsonConverter().convertJsonStrToObject(json);
        } catch (JsonProcessingException e) {
            log.error(e.getOriginalMessage());
        }
    }
}
