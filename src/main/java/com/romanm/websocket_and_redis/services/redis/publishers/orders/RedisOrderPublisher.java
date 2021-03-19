package com.romanm.websocket_and_redis.services.redis.publishers.orders;

import com.romanm.websocket_and_redis.models.orders.Order;


public interface RedisOrderPublisher {
    /**
     * * Метод публикации заказа
     * @param order Order
     * @return Order
     */
    Order publishOrder(Order order);
}
