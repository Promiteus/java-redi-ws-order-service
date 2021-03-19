package com.romanm.websocket_and_redis.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

@Configuration
public class CommonConfigs {
    @Bean
    public Environment environment() {
        return new StandardEnvironment();
    }
}
