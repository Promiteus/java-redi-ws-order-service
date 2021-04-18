package com.romanm.websocket_and_redis.controllers;

import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.responses.ResponseData;
import com.romanm.websocket_and_redis.models.responses.ResponseDeleteStatus;
import com.romanm.websocket_and_redis.models.responses.ResponseObjectsData;
import com.romanm.websocket_and_redis.services.redis.CrudDialService;
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

    private RedisDialPublisher redisDialPublisher;
    private CrudDialService crudDialService;

    @Autowired
    public DialController(RedisDialPublisher redisDialPublisher) {
        this.redisDialPublisher = redisDialPublisher;
        this.crudDialService = crudDialService;
    }

    @PostMapping(value = Prefixes.API_DIAL_PATH)
    public ResponseEntity<?> postDial(@RequestBody Dial dial) {
        log.info("DialController have got post order: {}", dial);
        return ResponseEntity.ok(new ResponseObjectsData(List.of(this.redisDialPublisher.publishDial(dial)), 200));
    }

    @DeleteMapping(value = Prefixes.API_DIAL_DELETE_BY_USER_PATH)
    public ResponseEntity<?> deleteUserDial(@RequestBody Dial dial) {
        log.info("DialController have got delete request from user: {}: ", dial);

        return ResponseEntity
                .ok()
                .body(new ResponseData(new ResponseDeleteStatus(this.redisDialPublisher.deleteDial(dial, true)), 200));
    }

    @DeleteMapping(value = Prefixes.API_DIAL_DELETE_BY_EXECUTOR_PATH)
    public ResponseEntity<?> deleteExecutorDial(@RequestBody Dial dial) {
        log.info("DialController have got delete request from executor: {}", dial);
        return ResponseEntity
                .ok()
                .body(new ResponseData(new ResponseDeleteStatus(this.redisDialPublisher.deleteDial(dial, false)), 200));
    }

    @GetMapping(value = Prefixes.API_DIAL_PATH+Prefixes.API_SLASH_ID)
    public ResponseEntity<?> getAllDialsOfExecutor(@PathVariable(Prefixes.API_ID) String selkod) {
        return ResponseEntity.ok(this.crudDialService.getAllDialsBySelkod(selkod));
    }

    @GetMapping(value = Prefixes.API_DIAL_PATH+Prefixes.API_PAGE+Prefixes.API_SLASH_ID)
    public ResponseEntity<?> getPageDialsOfExecutor(@PathVariable(Prefixes.API_ID) String selkod,
                                                    @PathVariable("page") long page,
                                                    @PathVariable("size") long pageSize) {
        return ResponseEntity.ok(this.crudDialService.getPageDialsBySelkod(selkod, page, pageSize));
    }
}
