package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.models.dials.Dial;

import java.util.List;
import java.util.Set;

public interface CrudDialService {
    Set<Dial> getAllDialsBySelkod(String selkod);
    Set<Dial> getPageDialsBySelkod(String selkod, long page, long pageSize);
}
