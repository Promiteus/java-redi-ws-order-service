package com.romanm.websocket_and_redis.configs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(BrokerConfiguration.class)
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
    private BrokerConfiguration brokerConfiguration;

    public WebSocketConfiguration(BrokerConfiguration brokerConfiguration) {
        this.brokerConfiguration = brokerConfiguration;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(brokerConfiguration.getEndpoint()).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(brokerConfiguration.getBroker());
        registry.setApplicationDestinationPrefixes(brokerConfiguration.getApp());
    }
}
