package com.romanm.websocket_and_redis.components.utils;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RedisSetStreamFilter {
    public Stream filterRedisSet(Set redisSet, String id) {
        return redisSet.stream().filter(item -> (item.toString().contains(id)));
    }

    public int filterRedisSetSize(Set redisSet, String id) {
        return filterRedisSet(redisSet, id).toArray().length;
    }
}
