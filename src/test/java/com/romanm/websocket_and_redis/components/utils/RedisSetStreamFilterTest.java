package com.romanm.websocket_and_redis.components.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
import com.romanm.websocket_and_redis.services.redis.RedisService;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisSetStreamFilterTest {
    @Autowired
    private RedisSetStreamFilter redisSetStreamFilter;
    @Autowired
    private RedisService redisService;
    @Autowired
    private OrderJsonConverter orderJsonConverter;

    @Test
    public void filterRedisSetTest() throws JsonProcessingException {
       Order order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск")
                .setUserkod(UUID.randomUUID().toString())
                .build();

       String userkod = KeyFormatter.hideHyphenChar(order.getUserkod());
       //Добавить тестовый заказ упорядоченное множество
       redisService.addZSetValue(userkod, orderJsonConverter.convertObjectToJson(order), new Date().getTime());
       //Повтор
       redisService.addZSetValue(userkod, orderJsonConverter.convertObjectToJson(order), new Date().getTime());

       //Проверить, есть ли во множестве заказ
       Stream objectStream = redisSetStreamFilter.filterRedisSet(redisService.getZSetRange(userkod, 0, -1), order.getId());
       log.info(Prefixes.TEST_PREFIX+"There are orders: "+objectStream.toArray().length);

       Stream objectStreamTest1 = redisSetStreamFilter.filterRedisSet(redisService.getZSetRange(userkod, 0, -1), order.getId());
       Assert.isTrue(objectStreamTest1.toArray().length == 1, "There should be only one order in ZSET!");

       //Удалить запись из множества
       redisService.removeZSetItem(userkod, orderJsonConverter.convertObjectToJson(order));

       int size = redisSetStreamFilter.filterRedisSetSize(redisService.getZSetRange(userkod, 0, -1), order.getId());
       Assert.isTrue(size == 0, "There should not any orders!");
    }
}