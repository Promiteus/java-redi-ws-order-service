package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.repositories.RedisRepository;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service(value = "crudServiceV1")
public class CrudDialServiceV1 implements CrudDialService {
    private RedisRepository redisRepository;
    private RedisService redisService;

    @Autowired
    public CrudDialServiceV1(RedisRepository redisRepository, RedisService redisService) {
        this.redisRepository = redisRepository;
        this.redisService = redisService;
    }

    @Override
    public Set<Dial> getAllDialsBySelkod(String selkod) {
        if (Optional.ofNullable(selkod).isPresent()) {
            return this.redisRepository.getZSetRange(KeyFormatter.hideHyphenChar(selkod), 0, -1);
        }
        return new HashSet<>();
    }

    @Override
    public Set getPageDialsBySelkod(String selkod, long page, long pageSize) {
       return this.redisService.getPagebleZSETValue(selkod, page, pageSize);
    }
}
