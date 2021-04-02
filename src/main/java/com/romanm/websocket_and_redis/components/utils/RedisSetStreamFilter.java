package com.romanm.websocket_and_redis.components.utils;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class RedisSetStreamFilter {
    public Stream filterRedisSet(Set redisSet, String id) {
        return redisSet.stream().filter(item -> (item.toString().contains(id)));
    }

    public Stream douleFilterRedisSet(Set redisSet, String id1, String id2) {
        return redisSet.stream().filter(item -> (item.toString().contains(id1))).filter(item -> (item.toString().contains(id2)));
    }

    public long doubleFilterRedisSetSize(Set redisSet, String id1, String id2) {
        return douleFilterRedisSet(redisSet, id1, id2).toArray().length;
    }

    public int filterRedisSetSize(Set redisSet, String id) {
        return filterRedisSet(redisSet, id).toArray().length;
    }
}
