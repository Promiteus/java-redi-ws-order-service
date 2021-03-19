package com.romanm.websocket_and_redis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romanm.websocket_and_redis.models.orders.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component(value = "orderJsonConverter")
public class OrderJsonConverter implements JsonConverter<Order>{

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertObjectToJson(Order object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    @Override
    public Order convertJsonStrToObject(String json) throws JsonProcessingException {
        return mapper.readValue(json, Order.class);
    }
}
