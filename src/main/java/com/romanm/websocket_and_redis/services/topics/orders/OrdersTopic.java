package com.romanm.websocket_and_redis.services.topics.orders;

import com.romanm.websocket_and_redis.components.RusCharsTranslator;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.services.topics.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrdersTopic implements Topic<Order> {

    private RusCharsTranslator rusCharsTranslator;

    @Autowired
    public OrdersTopic(RusCharsTranslator rusCharsTranslator) {
        this.rusCharsTranslator = rusCharsTranslator;
    }

    public String getTopic(Order order) {
        List<String> topicItems = List.of(
                rusCharsTranslator.translate(order.getCountry().toLowerCase()),
                rusCharsTranslator.translate(order.getRegion().toLowerCase()),
                rusCharsTranslator.translate(order.getLocality().toLowerCase())
        );
        return Prefixes.ORDER_PREFIX+String.join(".", topicItems);
    }
}
