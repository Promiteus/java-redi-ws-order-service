package com.romanm.websocket_and_redis.events.dials;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DialEventPublisher {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishDialEvent(String channel, String data) {
      log.info(String.format("Publishing dial event with channel [%s] and data [%s]", channel, data));
      DialPublishEvent dialPublishEvent = new DialPublishEvent(this, channel, data);
      applicationEventPublisher.publishEvent(dialPublishEvent);
    }
}
