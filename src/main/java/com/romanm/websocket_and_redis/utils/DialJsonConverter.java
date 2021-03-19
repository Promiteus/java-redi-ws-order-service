package com.romanm.websocket_and_redis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romanm.websocket_and_redis.models.dials.Dial;
import org.springframework.stereotype.Component;

@Component(value = "dialJsonConverter")
public class DialJsonConverter implements JsonConverter<Dial> {
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertObjectToJson(Dial object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    @Override
    public Dial convertJsonStrToObject(String json) throws JsonProcessingException {
        return mapper.readValue(json, Dial.class);
    }
}
