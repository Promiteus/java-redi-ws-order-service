package com.romanm.websocket_and_redis.events.orders;

import com.romanm.websocket_and_redis.components.WebsocketProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener implements ApplicationListener<OrderPublishEvent> {
    private WebsocketProducer websocketProducer;

    public OrderEventListener(WebsocketProducer websocketProducer) {
        this.websocketProducer = websocketProducer;
    }

    @Override
    public void onApplicationEvent(OrderPublishEvent orderPublishEvent) {
        //log.info(String.format("Sending order over websocket channel [%s] ", orderPublishEvent.getChannel()));
        this.websocketProducer.sendOrderDataToTopic(orderPublishEvent.getChannel(), orderPublishEvent.getData());
    }
}
