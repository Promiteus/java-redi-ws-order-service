package com.romanm.websocket_and_redis.services.redis.publishers.dials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
import com.romanm.websocket_and_redis.services.redis.RedisService;
import com.romanm.websocket_and_redis.services.redis.publishers.orders.RedisOrderPublisher;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Autowired
    private OrderJsonConverter orderJsonConverter;

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

    @Test
    @SneakyThrows
    public void publishDialTransactionParallelTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setStatus(Order.STATUS.NEW)
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
        log.info(Prefixes.TEST_PREFIX+"Order: "+this.redisService.getZSetRange(userkod, 0, -1));
        //Теперь из этого заказа нужно сделать сделку и отследить состояние связанных множеств в Redis
        //Создаем сделку
        //Dial dial = null;

        //Содаем сделку при одновременном обращении нескольки исполнителей
        //redisDialPublisher.publishDial(dial);
        ExecutorService service = Executors.newFixedThreadPool(5);


        String selkod = UUID.randomUUID().toString();
        Dial dial1 = new Dial(selkod, order);
        selkod = KeyFormatter.hideHyphenChar(selkod);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        service.execute(new DialCreator(redisDialPublisher, dial1));

        String selkod2 = UUID.randomUUID().toString();
        Dial dial2 = new Dial(selkod2, order);
        selkod2 = KeyFormatter.hideHyphenChar(selkod2);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod2+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod2));
        service.execute(new DialCreator(redisDialPublisher, dial2));

        String selkod3 = UUID.randomUUID().toString();
        Dial dial3 = new Dial(selkod3, order);
        selkod3 = KeyFormatter.hideHyphenChar(selkod3);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod3+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod3));
        service.execute(new DialCreator(redisDialPublisher, dial3));

        String selkod4 = UUID.randomUUID().toString();
        Dial dial4 = new Dial(selkod4, order);
        selkod4 = KeyFormatter.hideHyphenChar(selkod4);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod4+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod4));
        service.execute(new DialCreator(redisDialPublisher, dial4));

        String selkod5 = UUID.randomUUID().toString();
        Dial dial5 = new Dial(selkod5, order);
        selkod5 = KeyFormatter.hideHyphenChar(selkod5);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod5+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod5));
        service.execute(new DialCreator(redisDialPublisher, dial5));

        /*dial = new Dial(UUID.randomUUID().toString(), order);
        service.execute(new DialCreator(redisDialPublisher, dial));

        dial = new Dial(UUID.randomUUID().toString(), order);
        service.execute(new DialCreator(redisDialPublisher, dial));*/

        Thread.sleep(3000);

        log.info("\n\r");
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(userkod) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(userkod) get data: "+
                this.redisService.getZSetRange(userkod, 0, -1));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(region) Size: "+
                this.redisService.getZSetSize(topicRegion));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Order region: "+this.redisService.getZSetRange(topicRegion, 0, -1));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod2+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod2));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod3+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod3));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod4+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod4));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod5+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod5));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));

        Assert.isTrue(this.redisService.getZSetSize(userkod) == 1, "Orders ZSET(userkod) must have size = 1!");
        Assert.notEmpty(this.redisService.getZSetRange(userkod, 0, -1), "ZSET(userkod) must have one dial!");
        Assert.isTrue(this.redisService.getZSetSize(topicRegion) == 0, "ZSET(region) must be empty!");
        Assert.isTrue(this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()) != null, "HASH(PROCESSING) must have selkod!");
    }

    @Test
    @SneakyThrows
    public void publishDialTransactionSerialTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setStatus(Order.STATUS.NEW)
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
        log.info(Prefixes.TEST_PREFIX+"Oredr: "+this.redisService.getZSetRange(userkod, 0, -1));
        //Теперь из этого заказа нужно сделать сделку и отследить состояние связанных множеств в Redis
        //Создаем сделку
        //Dial dial = null;

        //Содаем сделку при одновременном обращении нескольки исполнителей
        //redisDialPublisher.publishDial(dial);

        String selkod = UUID.randomUUID().toString();
        Dial dial1 = new Dial(selkod, order);
        selkod = KeyFormatter.hideHyphenChar(selkod);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        redisDialPublisher.publishDial(dial1);

        String selkod2 = UUID.randomUUID().toString();
        Dial dial2 = new Dial(selkod2, order);
        selkod2 = KeyFormatter.hideHyphenChar(selkod2);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod2+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod2));
        redisDialPublisher.publishDial(dial2);

        String selkod3 = UUID.randomUUID().toString();
        Dial dial3 = new Dial(selkod3, order);
        selkod3 = KeyFormatter.hideHyphenChar(selkod3);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod3+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod3));
        redisDialPublisher.publishDial(dial3);

        String selkod4 = UUID.randomUUID().toString();
        Dial dial4 = new Dial(selkod4, order);
        selkod4 = KeyFormatter.hideHyphenChar(selkod4);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod4+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod4));
        redisDialPublisher.publishDial(dial4);

        String selkod5 = UUID.randomUUID().toString();
        Dial dial5 = new Dial(selkod5, order);
        selkod5 = KeyFormatter.hideHyphenChar(selkod5);
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL] Dials ZSET("+selkod5+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod5));
        redisDialPublisher.publishDial(dial5);

        /*dial = new Dial(UUID.randomUUID().toString(), order);
        service.execute(new DialCreator(redisDialPublisher, dial));

        dial = new Dial(UUID.randomUUID().toString(), order);
        service.execute(new DialCreator(redisDialPublisher, dial));*/

        Thread.sleep(3000);

        log.info("\n\r");
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(userkod) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(userkod) get data: "+
                this.redisService.getZSetRange(userkod, 0, -1));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Orders ZSET(region) Size: "+
                this.redisService.getZSetSize(topicRegion));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod2+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod2));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod3+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod3));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod4+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod4));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials ZSET("+selkod5+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod5));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));
    }

    @Test
    public void deleteDialByUser() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setStatus(Order.STATUS.NEW)
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
        log.info(Prefixes.TEST_PREFIX+"Order: "+this.redisService.getZSetRange(userkod, 0, -1));


        String selkod = UUID.randomUUID().toString();
        Dial dial1 = new Dial(selkod, order);
        selkod = KeyFormatter.hideHyphenChar(selkod);

        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));

        redisDialPublisher.publishDial(dial1);

        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));

        redisDialPublisher.deleteDial(dial1, true);

        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL userkod] Order ZSET("+userkod+", ORDER) Size: "+
                this.redisService.getZSetRange(userkod, 0, -1));
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));

        Assert.isTrue(this.redisService.getZSetSize(selkod) == 0, "Dial SET of executor must be empty!");
        Assert.isTrue(this.redisService.getZSetSize(userkod) == 0, "Order SET of user must be empty!");
        Assert.isTrue(this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()) == null, "Dial HASHMAP of executor must be empty!");
    }

    @Test
    public void deleteDialByExecutor() throws JsonProcessingException {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

        //Создаем объект заказа
        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setStatus(Order.STATUS.NEW)
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
        log.info(Prefixes.TEST_PREFIX+"Order: "+this.redisService.getZSetRange(userkod, 0, -1));


        String selkod = UUID.randomUUID().toString();
        Dial dial1 = new Dial(selkod, order);
        selkod = KeyFormatter.hideHyphenChar(selkod);

        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[Before publish DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));

        redisDialPublisher.publishDial(dial1);

        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After publish DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));

        redisDialPublisher.deleteDial(dial1, false);

        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL selkod] Dials ZSET("+selkod+", DIAL) Size: "+
                this.redisService.getZSetSize(selkod));
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL userkod] Dials ZSET("+userkod+", DIAL) Size: "+
                this.redisService.getZSetSize(userkod));
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL userkod] Order object ("+userkod+", ORDER) Size: "+
                this.redisService.getZSetRange(userkod, 0, -1).toArray()[0]);
        log.info(Prefixes.TEST_PREFIX+"[After deleting DIAL] Dials HASH(PROCESSING) Values: "+
                this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()));

        Assert.isTrue(this.redisService.getZSetSize(selkod) == 0, "Dial SET of executor must be empty!");
        Assert.isTrue(this.redisService.getZSetSize(userkod) == 1, "Order SET of user must have one record!");
        Assert.isTrue(this.redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial1.getOrder().getId()) == null, "Dial HASHMAP of executor must be empty!");
        Assert.isTrue(orderJsonConverter.convertJsonStrToObject(this.redisService.getZSetRange(userkod, 0, -1).toArray()[0].toString()).getStatus() == Order.STATUS.NEW, "Order SET of user must have order status is NEW!");
    }

    static class DialCreator implements Runnable {

        private RedisDialPublisher rdp;
        private Dial dial;

        public DialCreator(RedisDialPublisher rdp, Dial dial) {
            this.dial = dial;
            this.rdp = rdp;
        }


        @Override
        public void run() {
            log.info(Prefixes.TEST_PREFIX+this.getClass().getSimpleName()+" Puplishing dial! Seconds: "+ System.currentTimeMillis()/1000);
            Dial dialRes = rdp.publishDial(dial);
            //log.info(Thread.currentThread().getId()+" "+dialRes);
        }
    }
}