package com.romanm.websocket_and_redis.events.dials;

import com.romanm.websocket_and_redis.components.WebsocketProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DialEventListener implements ApplicationListener<DialPublishEvent> {
    private WebsocketProducer websocketProducer;

    public DialEventListener(WebsocketProducer websocketProducer) {
        this.websocketProducer = websocketProducer;
    }

    @Override
    public void onApplicationEvent(DialPublishEvent orderPublishEvent) {
       // log.info(String.format("Sending Dial over websocket channel [%s] ", orderPublishEvent.getChannel()));
        this.websocketProducer.sendDialDataToTopic(orderPublishEvent.getChannel(), orderPublishEvent.getData());
    }
}
