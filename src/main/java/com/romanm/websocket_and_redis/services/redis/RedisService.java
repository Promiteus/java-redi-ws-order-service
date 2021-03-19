package com.romanm.websocket_and_redis.services.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.Set;

public interface RedisService {
    Long removeSetItem(String name, Object item);
    Long addToSet(String name, String item);
    boolean itemSetExists(String name, String item);
    public Set getSetMembers(String name);
    void putHashMapItem(String name, String key, Object object);
    Object getHashMapItem(String name, String key);
    Map getHashMapEnries(String name);
    void setKeyValue(String key, Object value);
    Object getValueByKey(String key);
    boolean removeValueByKey(String key);
    Integer appendValue(Object key, String value);
    boolean addZSetValue(Object key, Object value, double v);
    Set getZSetRange(Object key, long from, long to);
    Long getZSetSize(Object key);
    Long removeZSetItem(Object key, Object... items);

    RedisTemplate getRedisTemplate();
}
