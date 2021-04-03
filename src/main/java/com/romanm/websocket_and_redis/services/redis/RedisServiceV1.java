package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.repositories.RedisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("redisServiceV1")
public class RedisServiceV1 implements RedisService {
    private RedisRepository redisRepository;

    @Autowired
    public RedisServiceV1(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public void watch(Object key) { redisRepository.watch(key); }

    /*Redis SET(NAME, VALUE) - упорядоченное множество.*/
    //-------------------------------------------------
    @Override
    public Long removeSetItem(String name, Object item) {
        return this.redisRepository.removeSetItem(name, item);
    }

    @Override
    public Long addToSet(String name, String item) {
        return this.redisRepository.addToSet(name, item);
    }

    @Override
    public boolean itemSetExists(String name, String item) {
        return  this.redisRepository.itemSetExists(name, item);
    }

    @Override
    public Set getSetMembers(String name) {
        return this.redisRepository.getSetMembers(name);
    }

    //-------------------------------------------------

    /*Redis HashMap(NAME, KEY, VALUE) - хэш карта ключ - значение.*/
    //-------------------------------------------------
    @Override
    public void putHashMapItem(String name, String key, Object object) {
        this.redisRepository.putHashMapItem(name, key, object);
    }

    @Override
    public Object getHashMapItem(String name, String key) {
        return this.redisRepository.getHashMapItem(name, key);
    }

    @Override
    public Map getHashMapEnries(String name) {
        return this.redisRepository.getHashMapEnries(name);
    }
    //-------------------------------------------------

    /*Redis SET(KEY, VALUE) - ассоциативный массив ключ - значение.*/
    //-------------------------------------------------
    @Override
    public void setKeyValue(String key, Object value) {
        this.redisRepository.setKeyValue(key, value);
    }

    @Override
    public Object getValueByKey(String key) {
        return this.redisRepository.getValueByKey(key);
    }

    @Override
    public boolean removeValueByKey(String key) {
        return this.redisRepository.removeValueByKey(key);
    }

    @Override
    public Integer appendValue(Object key, String value) {
        return this.redisRepository.appendValue(key, value);
    }
    //-------------------------------------------------

    /*Redis ZSET(KEY, VALUE, WEIGHT) - отсортированное множество.*/
    //-------------------------------------------------
    @Override
    public boolean addZSetValue(Object key, Object value, double v) { return this.redisRepository.addZSetValue(key, value, v); }

    @Override
    public Set getZSetRange(Object key, long from, long to) {
        return this.redisRepository.getZSetRange(key, from, to);
    }

    @Override
    public Long getZSetSize(Object key) {
        return this.redisRepository.getZSetSize(key);
    }

    @Override
    public Long removeZSetItem(Object key, Object... items) {
        return this.redisRepository.removeZSetItem(key, items);
    }

    @Override
    public void multi() {
        this.redisRepository.multi();
    }

    @Override
    public List<Object> exec() {
        return this.redisRepository.exec();
    }

    @Override
    public void discard() {
        this.redisRepository.discard();
    }
    //-------------------------------------------------

    @Override
    public RedisTemplate getRedisTemplate() {
        return this.redisRepository.getRedisTemplate();
    }

}
