package com.romanm.websocket_and_redis.models.dials;

import com.romanm.websocket_and_redis.models.orders.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dial {
    private String selkod;
    private Order order;
}
