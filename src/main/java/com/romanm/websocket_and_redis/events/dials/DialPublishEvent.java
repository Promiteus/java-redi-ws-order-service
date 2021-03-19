package com.romanm.websocket_and_redis.events.dials;

import org.springframework.context.ApplicationEvent;

public class DialPublishEvent extends ApplicationEvent {
    private String channel;
    private String data;

    public DialPublishEvent(Object source, String channel, String data) {
        super(source);
        this.channel = channel;
        this.data = data;
    }

    public String getChannel() {
        return channel;
    }

    public String getData() {
        return data;
    }
}
