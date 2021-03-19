package com.romanm.websocket_and_redis.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "service.ws")
public class BrokerConfiguration {
    private String app = "/app-orders-api-ws";
    private String broker = "/notifier";
    private String endpoint = "/stomp";
    private String orderKeyName = "orders";
}
