package com.romanm.websocket_and_redis.services.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import org.springframework.data.redis.core.RedisOperations;

public interface DialOrderHandler {
    boolean isSuccessDial(Dial dial);
    void notifyDialOrderStatus(Dial dial, boolean success) throws JsonProcessingException;
    void notifyDialStatus(Dial dial, boolean toExecutor) throws JsonProcessingException;
    void notifyOrderStatus(Order order) throws JsonProcessingException;
    void deleteOrderOps(RedisOperations redisOps, Dial dial) throws JsonProcessingException;
    void addOrder(RedisOperations redisOps, Order order) throws JsonProcessingException;
    void removeDial(RedisOperations redisOps, Dial dial) throws JsonProcessingException;
}
