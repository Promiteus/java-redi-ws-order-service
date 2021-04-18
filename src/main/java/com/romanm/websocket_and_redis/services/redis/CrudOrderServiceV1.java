package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.repositories.RedisRepository;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service(value = "crudOrderServiceV1")
public class CrudOrderServiceV1 implements CrudOrderService {
    private RedisRepository redisRepository;
    private RedisService redisService;

    @Autowired
    public CrudOrderServiceV1(RedisRepository redisRepository, RedisService redisService) {
        this.redisRepository = redisRepository;
        this.redisService = redisService;
    }

    @Override
    public Set<Order> getAllOrdersByRegion(String region) {
        if (Optional.ofNullable(region).isPresent()) {
            return this.redisRepository.getZSetRange(region, 0, -1);
        }
        return new HashSet<>();
    }

    @Override
    public Set getPageOrdersByRegion(String region, long page, long pageSize) {
        return this.redisService.getPagebleZSETValue(region, page, pageSize);
    }

    @Override
    public Set<Order> getAllOrdersByUserkod(String userkod) {
        if (Optional.ofNullable(userkod).isPresent()) {
            return this.redisRepository.getZSetRange(KeyFormatter.hideHyphenChar(userkod), 0, -1);
        }
        return new HashSet<>();
    }

    @Override
    public Set getPageOrdersByUserkod(String userkod, long page, long pageSize) {
        return this.redisService.getPagebleZSETValue(userkod, page, pageSize);
    }
}
