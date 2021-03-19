package com.romanm.websocket_and_redis.models.dials;

import com.romanm.websocket_and_redis.models.orders.Order;
import lombok.Data;

@Data
public class Dial {
    private String selkod;
    private Order order;
}
