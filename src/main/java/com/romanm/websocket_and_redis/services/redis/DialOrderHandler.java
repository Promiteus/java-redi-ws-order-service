package com.romanm.websocket_and_redis.services.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.models.dials.Dial;
import org.springframework.data.redis.core.RedisOperations;

public interface DialOrderHandler {
    boolean isSuccessDial(Dial dial);
    void notifyDialOrderStatus(Dial dial, boolean success) throws JsonProcessingException;
    void deleteOrderOps(RedisOperations redisOps, Dial dial) throws JsonProcessingException;
}
