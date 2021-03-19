package com.romanm.websocket_and_redis.components.utils;

import java.util.Set;
import java.util.stream.Stream;

public class RedisSetStreamFilter {
    public Stream filterRedisSet(Set redisSet, String id) {
        return redisSet.stream().filter(item -> (item.toString().contains(id)));
    }
}
