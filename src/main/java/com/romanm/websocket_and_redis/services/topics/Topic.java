package com.romanm.websocket_and_redis.services.topics;

public interface Topic<T> {
    public String getTopic(T object);
}
