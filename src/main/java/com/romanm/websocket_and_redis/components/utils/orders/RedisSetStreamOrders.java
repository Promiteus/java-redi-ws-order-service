package com.romanm.websocket_and_redis.components.utils.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.components.utils.RedisSetStreamFilter;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.utils.JsonConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RedisSetStreamOrders extends RedisSetStreamFilter {

    private JsonConverter jsonConverter;

    @Autowired
    public RedisSetStreamOrders(@Qualifier("orderJsonConverter") JsonConverter jsonConverter) {
       this.jsonConverter = jsonConverter;
    }

    public List<Order> getOrdersFromRedisSet(Set rediSet, String filterId)  {
        return (List<Order>) super.filterRedisSet(rediSet, filterId).map(item -> {
            try {
                return (jsonConverter.convertJsonStrToObject(item.toString()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
    }
}
