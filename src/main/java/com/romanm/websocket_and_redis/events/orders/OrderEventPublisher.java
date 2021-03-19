package com.romanm.websocket_and_redis.events.orders;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventPublisher {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishOrderEvent(String channel, String data) {
      log.info(String.format("Publishing order event with channel [%s] and data [%s]", channel, data));
      OrderPublishEvent orderPublishEvent = new OrderPublishEvent(this, channel, data);
      applicationEventPublisher.publishEvent(orderPublishEvent);
    }
}
