package com.romanm.websocket_and_redis.controllers;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
import com.romanm.websocket_and_redis.models.responses.ResponseObjectsData;
import com.romanm.websocket_and_redis.services.redis.publishers.orders.RedisOrderPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = Prefixes.API_BASE_PATH)
public class OrderController {

    RedisOrderPublisher redisOrderPublisher;

    @Autowired
    public OrderController(RedisOrderPublisher redisOrderPublisher) {
       this.redisOrderPublisher = redisOrderPublisher;
    }

    @PostMapping(value = Prefixes.API_ORDER_PATH)
    public ResponseEntity<?> postOrder(@RequestBody Order order) {
        log.info("OrderController have got post order: {}", order);

        order = OrderBuilder.create(order).setOrderId("9002-ad02-2211").setCode(10998).build();

        return ResponseEntity.ok(new ResponseObjectsData(List.of(this.redisOrderPublisher.publishOrder(order)), 200));
    }

    @DeleteMapping(value = Prefixes.API_ORDER_PATH)
    public ResponseEntity<?> deleteOrder(@RequestBody Order order) {
        log.info("OrderController have got delete request for id: "+order.getId());

        return ResponseEntity.ok().body(new ResponseObjectsData(List.of(this.redisOrderPublisher.deleteOrder(order)), 200));
    }

}
