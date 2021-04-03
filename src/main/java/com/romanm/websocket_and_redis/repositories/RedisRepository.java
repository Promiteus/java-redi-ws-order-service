package com.romanm.websocket_and_redis.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * * Репозиторий для работы с низкоуровневыми командами Redis
 */
@Repository
public class RedisRepository {
    RedisTemplate redisTemplate;

    /**
     *
     * @param redisTemplate RedisTemplate
     */
    @Autowired
    public RedisRepository(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * * Получить экземпляр шаблона для работы с функциями Redis
     * @return RedisTemplate
     */
    public RedisTemplate getRedisTemplate() {
        return this.redisTemplate;
    }

    /**
     * Add data to SET by name
     * @param name String
     * @param item String
     * @return Long
     */
    public Long addToSet(String name, String item) {
        return this.redisTemplate.opsForSet().add(name, item);
    }

    /**
     * Remove data from SET
     * @param name String
     * @param item String
     * @return Long
     */
    public Long removeSetItem(String name, Object item) {
        return this.redisTemplate.opsForSet().remove(name, item);
    }


    /**
     *
     * @param name String
     * @param item String
     * @return boolean
     */
    public boolean itemSetExists(String name, String item) {
         return this.redisTemplate.opsForSet().isMember(name, item);
    }

    /**
     *
     * @param name String
     * @return Set
     */
    public Set getSetMembers(String name) {
       return this.redisTemplate.opsForSet().members(name);
    }

    /**
     * * Полодить данный в карту, установив предварительно ее имя.
     * @param name String
     * @param key String
     * @param object Object
     */
    public void putHashMapItem(String name, String key, Object object) {
        this.redisTemplate.opsForHash().put(name, key, object);
    }

    /**
     * * Поучить данные из карты.
     * @param name String
     * @param key String
     * @return Object
     */
    public Object getHashMapItem(String name, String key) {
        return this.redisTemplate.opsForHash().get(name, key);
    }

    /**
     * * Получить все данные по имени карты.
     * @param name String
     * @return Map
     */
    public Map getHashMapEnries(String name) {
        return this.redisTemplate.opsForHash().entries(name);
    }

    /**
     * * Ассоциативный массив. Задать пару ключ - значение.
     * @param key String
     * @param value Object
     */
    public void setKeyValue(String key, Object value) {
        this.redisTemplate.opsForValue().set(key, value);
    }

    /**
     * * Ассоциативный массив. Получить значение по ключу.
     * @param key String
     * @return Object
     */
    public Object getValueByKey(String key) {
        return this.redisTemplate.opsForValue().get(key);
    }

    /**
     * * Ассоциативный массив. Удалить значение по ключу
     * @param key String
     * @return boolean
     */
    public boolean removeValueByKey(String key) {
        return this.redisTemplate.opsForValue().getOperations().delete(key);
    }



    /**
     * * Ассоциативный массив. Добавить в массив пару ключ - значение.
     * @param key Object
     * @param value String
     */
    public Integer appendValue(Object key, String value) {
        return this.redisTemplate.opsForValue().append(key, value);
    }


    /**
     * * Добавляет в упорядоченное множество новое значение + весовой коэффициент v.
     *  * По коэффициенту v будет осуществляться сортировка.
     * @param key Object
     * @param value Object
     * @param v double
     * @return boolean
     */
    public boolean addZSetValue(Object key, Object value, double v) {
        return this.redisTemplate.opsForZSet().add(key, value, v);
    }

    /**
     * * Получить элементы множества от и до. Если to = -1, а from = 0,
     * * то метод выдаст все элементы, записанные во множестве
     * @param key Object
     * @param from Long
     * @param to Long
     * @return Set
     */
    public Set getZSetRange(Object key, long from, long to) {
       return this.redisTemplate.opsForZSet().range(key, from, to);
    }

    /**
     *
     * @param key Object
     * @return Long
     */
    public Long getZSetSize(Object key) {
        return this.redisTemplate.opsForZSet().zCard(key);
    }

    /**
     *
     * @param key Object
     * @param items Object...
     * @return Long
     */
    public Long removeZSetItem(Object key, Object... items) {
        return this.redisTemplate.opsForZSet().remove(key, items);
    }

    /**
     *
     * @param key Object
     */
    public void watch(Object key) {
        this.redisTemplate.watch(key);
    }

    /**
     *
     */
    public void multi() {
        this.redisTemplate.multi();
    }

    /**
     *
     * @return List<Object>
     */
    public List<Object> exec() {
       return this.redisTemplate.exec();
    }

    public void discard() {
        this.redisTemplate.discard();
    }
}
