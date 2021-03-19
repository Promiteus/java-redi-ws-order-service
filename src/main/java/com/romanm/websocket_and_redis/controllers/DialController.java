package com.romanm.websocket_and_redis.controllers;

import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.responses.ResponseObjectData;
import com.romanm.websocket_and_redis.services.redis.publishers.dials.RedisDialPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = Prefixes.API_BASE_PATH)
public class DialController {

    private RedisDialPublisher redisDialBublisher;

    @Autowired
    public DialController(RedisDialPublisher redisDialBublisher) {
        this.redisDialBublisher = redisDialBublisher;
    }

    @PostMapping(value = Prefixes.API_DIAL_PATH)
    public ResponseEntity<?> postDial(@RequestBody Dial dial) {
        log.info("DialController have got post order: {}", dial);


        return ResponseEntity.ok(new ResponseObjectData(List.of(this.redisDialBublisher.publishDial(dial)), 200));
    }

    @DeleteMapping(value = Prefixes.API_DIAL_DELETE_BY_USER_PATH)
    public ResponseEntity<?> deleteUserDial(@PathVariable(Prefixes.API_ID) String id) {
        log.info("DialController have got delete request from user for id: "+id);

        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping(value = Prefixes.API_DIAL_DELETE_BY_EXECUTOR_PATH)
    public ResponseEntity<?> deleteExecutorDial(@PathVariable(Prefixes.API_ID) String id) {
        log.info("DialController have got delete request from user for id: "+id);

        return ResponseEntity.ok().body(null);
    }
}
