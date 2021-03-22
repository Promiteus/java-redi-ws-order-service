package com.romanm.websocket_and_redis.services.redis.publishers.dials;

import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
import com.romanm.websocket_and_redis.services.redis.RedisService;
import com.romanm.websocket_and_redis.services.redis.publishers.orders.RedisOrderPublisher;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.UUID;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisDialPublisherV1Test {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Topic<Order> topic;

    @Autowired
    private RedisDialPublisher redisDialPublisher;

    @Autowired
    private RedisOrderPublisher redisOrderPublisher;

    @Test
    public void publishDialIfUncknownOrder() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск")
                .setUserkod(UUID.randomUUID().toString())
                .build();

        //Теперь из этого заказа нужно сделать сделку и отследить состояние связанных множеств в Redis
        //Создаем сделку
        Dial dial = new Dial("3900-4432-aaff54", order);
        //Содаем сделку в Redis на базе Order и уведобляем об этом по каналу WebSocket
        redisDialPublisher.publishDial(dial);

    }

    @Test
    public void publishDialIfKnownOrder() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
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
        String topicRegion = topic.getTopic(order);

        log.info(Prefixes.TEST_PREFIX+"[Before publish ORDER] Orders ZSET(userkod, ORDER) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[Before publish ORDER] Orders ZSET(region, ORDER) Size: "+
                this.redisService.getZSetSize(topicRegion));


        //Создается 1 заказ
        redisOrderPublisher.publishOrder(order);

        log.info(Prefixes.TEST_PREFIX+"[After publish ORDER] Orders ZSET(userkod) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish ORDER] Orders ZSET(region) Size: "+
                this.redisService.getZSetSize(topicRegion));

        //Теперь из этого заказа нужно сделать сделку и отследить состояние связанных множеств в Redis
        //Создаем сделку
        String selkod = UUID.randomUUID().toString();
        Dial dial = new Dial(selkod, order);
        selkod = KeyFormatter.hideHyphenChar(selkod);

        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET(selkod, DIAL) Size: "+
                this.redisService.getZSetSize(selkod));

        //Содаем сделку в Redis на базе Order и уведобляем об этом по каналу WebSocket
        redisDialPublisher.publishDial(dial);

        log.info("\n\r");
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(userkod) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(region) Size: "+
                this.redisService.getZSetSize(topicRegion));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET(selkod, DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId()));

        Assert.isTrue(this.redisService.getZSetSize(userkod) == 1,
                "Orders/Dials ZSET(userkod) Size must be equals to 1");
        Assert.isTrue(this.redisService.getZSetSize(topicRegion) == 0,
                "Orders ZSET(region, ORDER) Size must be equals 0");
        Assert.isTrue( this.redisService.getZSetSize(selkod) == 1,
                "Orders ZSET(selkod, DIAL) Size must be equals 1");
        Assert.isTrue( this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId()).equals(dial.getSelkod()),
                "Dials HASH(PROCESSING) Values must be equals "+dial.getSelkod());
    }
}