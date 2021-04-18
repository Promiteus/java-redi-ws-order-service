package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.models.orders.Order;

import java.util.Set;

public interface CrudOrderService {
    Set<Order> getAllOrdersByRegion(String region);
    Set<Order> getPageOrdersByRegion(String region, long page, long pageSize);
    Set<Order> getAllOrdersByUserkod(String userkod);
    Set<Order> getPageOrdersByUserkod(String userkod, long page, long pageSize);
}
