package com.romanm.websocket_and_redis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface JsonConverter<T> {
    public String convertObjectToJson(T object) throws JsonProcessingException;
    public T convertJsonStrToObject(String json) throws JsonProcessingException;
}
