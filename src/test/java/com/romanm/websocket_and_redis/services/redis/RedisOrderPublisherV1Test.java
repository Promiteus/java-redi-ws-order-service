package com.romanm.websocket_and_redis.services.redis;

import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
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

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisOrderPublisherV1Test {

    @Autowired
    private RedisOrderPublisher redisOrderPublisher;

    @Autowired
    private RedisService redisService;

    @Autowired
    private Topic<Order> topic;


    /**
     * Тест бубликации заказа в Redis структуру для двух одинаковых пользователей и одинаковым регионом*/
    @Test
    public void OrderPublisherForTwoSameOrdersTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order = null;

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

        //Создается 2-а одинаковых заказа
        redisOrderPublisher.publishOrder(order);
        redisOrderPublisher.publishOrder(order);

        String keyName = KeyFormatter.hideChar(order.getUserkod(), "-");
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count: "+redisService.getZSetSize(keyName));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count: {}", redisService.getZSetRange(keyName, 0, -1));

        String topicValue = topic.getTopic(order);
        log.info(Prefixes.TEST_PREFIX+"Region orders count: "+redisService.getZSetSize(topicValue));
        log.info(Prefixes.TEST_PREFIX+"Region orders count: {}", redisService.getZSetRange(topicValue, 0, -1));

        Assert.isTrue(redisService.getZSetSize(keyName) == 1, "Должен быть размещен один заказ пользователя при попытке добавить два одинаковых.");
        Assert.isTrue(redisService.getZSetRange(topicValue, 0, -1).size() == 1, "Должен быть размещен один заказ пользователя для региона при попытке добавить два одинаковых");
    }

    /**
     * Тест бубликации заказа в Redis структуру для двух разных пользователей и одинаковым регионом*/
    @Test
    public void OrderPublisherForTwoDiffOrdersTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order;

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

        redisOrderPublisher.publishOrder(order);

        order = OrderBuilder
                .create()
                .setOrderId("345-ad34-4556-a383")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 121")
                .setLocality("Хабаровск")
                .setUserkod(UUID.randomUUID().toString())
                .build();

        redisOrderPublisher.publishOrder(order);

        String keyName = KeyFormatter.hideChar(order.getUserkod(), "-");
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count: "+redisService.getZSetSize(keyName));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count: {}", redisService.getZSetRange(keyName, 0, -1));

        String topicValue = topic.getTopic(order);
        log.info(Prefixes.TEST_PREFIX+"Region orders count: "+redisService.getZSetSize(topicValue));
        log.info(Prefixes.TEST_PREFIX+"Region orders count: {}", redisService.getZSetRange(topicValue, 0, -1));

        Assert.isTrue(redisService.getZSetSize(keyName) == 1,
                "Должен быть размещен один заказ пользователя при попытке добавить два одинаковых.");
        Assert.isTrue(redisService.getZSetSize(topicValue) == 2,
                "Должны быть размещены два заказ пользователя для региона при попытке добавить два разных пользоватлеля с одинаковым регионом");
    }

    /**
     * Тест бубликации заказа в Redis структуру для двух разных пользователей и разными регионами*/
    @Test
    public void OrderPublisherForTwoDiffOrdersAndRegionsTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order1 = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск")
                .setUserkod(UUID.randomUUID().toString())
                .build();

        Order order2 = OrderBuilder
                .create()
                .setOrderId("778-000-aaf1-a383")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 121")
                .setLocality("Бикин")
                .setUserkod(UUID.randomUUID().toString())
                .build();

        redisOrderPublisher.publishOrder(order1);
        redisOrderPublisher.publishOrder(order2);

        String keyName1 = KeyFormatter.hideChar(order1.getUserkod(), "-");
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: "+redisService.getZSetSize(keyName1));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: {}", redisService.getZSetRange(keyName1, 0, -1));
        String topicValue1 = topic.getTopic(order1);
        log.info(Prefixes.TEST_PREFIX+"Region orders count for Order1: "+redisService.getZSetSize(topicValue1));
        log.info(Prefixes.TEST_PREFIX+"Region orders count for Order1: {}", redisService.getZSetRange(topicValue1, 0, -1));
        log.info(String.format("Order1 [user: %s] [topic:%s]: ", keyName1, topicValue1));


        String keyName2 = KeyFormatter.hideChar(order2.getUserkod(), "-");
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: "+redisService.getZSetSize(keyName2));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: {}", redisService.getZSetRange(keyName2, 0, -1));
        String topicValue2 = topic.getTopic(order2);
        log.info(Prefixes.TEST_PREFIX+"Region orders count for Order2: "+redisService.getZSetSize(topicValue2));
        log.info(Prefixes.TEST_PREFIX+"Region orders count for Order2: {}", redisService.getZSetRange(topicValue2, 0, -1));
        log.info(String.format("Order2 [user: %s] [topic:%s]: ", keyName2, topicValue2));

        Assert.isTrue(redisService.getZSetSize(keyName1) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order1.getUserkod()));
        Assert.isTrue(redisService.getZSetSize(topicValue1) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа для темы '%s'!", order1.getUserkod(), topic.getTopic(order1)));

        Assert.isTrue(redisService.getZSetSize(keyName2) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order2.getUserkod()));
        Assert.isTrue(redisService.getZSetSize(topicValue2) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа для темы '%s'!", order2.getUserkod(), topic.getTopic(order2)));
    }


    /**
     * Тест бубликации заказа в Redis структуру для двух разных пользователей и c одинаковыми регионами*/
    @Test
    public void OrderPublisherForTwoDiffOrdersAndSameRegionsTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order1 = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск") //Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();


        Order order2 = OrderBuilder
                .create()
                .setOrderId("778-000-aaf1-a383")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 121")
                .setLocality("Хабаровск") //И Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();


        redisOrderPublisher.publishOrder(order1);
        redisOrderPublisher.publishOrder(order2);

        String keyName1 = KeyFormatter.hideHyphenChar(order1.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: "+redisService.getZSetSize(keyName1));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: {}", redisService.getZSetRange(keyName1, 0, -1));

        String keyName2 = KeyFormatter.hideHyphenChar(order2.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: "+redisService.getZSetSize(keyName2));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: {}", redisService.getZSetRange(keyName2, 0, -1));

        String topicValue1 = topic.getTopic(order1);
        log.info(Prefixes.TEST_PREFIX+"Topic ["+topicValue1+"] orders count: "+redisService.getZSetSize(topicValue1));
        log.info(Prefixes.TEST_PREFIX+"Topic ["+topicValue1+"] orders count: {}", redisService.getZSetRange(topicValue1, 0, -1));

        Assert.isTrue(redisService.getZSetSize(keyName1) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order1.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(keyName2) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order2.getUserkod()));

        //Темы у order1 и order2 одинаковые (темы - комбинация названий страны.края.города)
        Assert.isTrue(redisService.getZSetSize(topicValue1) == 2,
                String.format("Для темы '%s' должно быть две записи!", topic.getTopic(order1)));
    }

    /**
     * Тест бубликации заказа в Redis структуру для 4-х разных пользователей и одинаковыми регионами*/
    @Test
    public void OrderPublisherForTwoPairsDiffOrdersAndSameRegionsTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order1 = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск") //Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();


        Order order2 = OrderBuilder
                .create()
                .setOrderId("778-000-aaf1-a383")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 121")
                .setLocality("Хабаровск") //И Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();

        Order order3 = OrderBuilder
                .create()
                .setOrderId("411-891-aaf1-8999")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 333")
                .setLocality("Хабаровск") //И Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();

        Order order4 = OrderBuilder
                .create()
                .setOrderId("6558-778-aaf1-24456")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 1011")
                .setLocality("Хабаровск") //И Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();

        redisOrderPublisher.publishOrder(order1);
        redisOrderPublisher.publishOrder(order2);
        redisOrderPublisher.publishOrder(order3);
        redisOrderPublisher.publishOrder(order4);

        String keyName1 = KeyFormatter.hideHyphenChar(order1.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: "+redisService.getZSetSize(keyName1));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order1: {}", redisService.getZSetRange(keyName1, 0, -1));

        String keyName2 = KeyFormatter.hideHyphenChar(order2.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: "+redisService.getZSetSize(keyName2));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order2: {}", redisService.getZSetRange(keyName2, 0, -1));

        String keyName3 = KeyFormatter.hideHyphenChar(order3.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order3: "+redisService.getZSetSize(keyName3));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order3: {}", redisService.getZSetRange(keyName3, 0, -1));

        String keyName4 = KeyFormatter.hideHyphenChar(order4.getUserkod());
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order4: "+redisService.getZSetSize(keyName4));
        log.info(Prefixes.TEST_PREFIX+"Userkod orders count for Order4: {}", redisService.getZSetRange(keyName4, 0, -1));

        String topicValue1 = topic.getTopic(order1);
        log.info(Prefixes.TEST_PREFIX+"Topic ["+topicValue1+"] orders count: "+redisService.getZSetSize(topicValue1));
        log.info(Prefixes.TEST_PREFIX+"Topic ["+topicValue1+"] orders count: {}", redisService.getZSetRange(topicValue1, 0, -1));

        Assert.isTrue(redisService.getZSetSize(keyName1) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order1.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(keyName2) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order2.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(keyName3) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order3.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(keyName4) == 1,
                String.format("Пользователь '%s' должен иметь одну запись заказа!", order4.getUserkod()));

        //Темы у order1-4 одинаковые (темы - комбинация названий страны.края.города)
        Assert.isTrue(redisService.getZSetSize(topicValue1) == 4,
                String.format("Для темы '%s' должно быть 4-е записи!", topic.getTopic(order1)));
    }

    @Test
    public void deleteOrderTest() {
        redisService.getRedisTemplate().getConnectionFactory().getConnection().flushAll();

        Order order1 = OrderBuilder
                .create()
                .setOrderId("345-ad34-44")
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск") //Хабаровск
                .setUserkod(UUID.randomUUID().toString())
                .build();


        Order order2 = OrderBuilder
                .create()
                .setOrderId("778-000-aaf1-a383")
                .setCode(3477)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 121")
                .setLocality("Амурск") 
                .setUserkod(UUID.randomUUID().toString())
                .build();

        redisOrderPublisher.publishOrder(order1);
        redisOrderPublisher.publishOrder(order1);
        redisOrderPublisher.publishOrder(order2);
        redisOrderPublisher.publishOrder(order2);

        String userkod1 = KeyFormatter.hideHyphenChar(order1.getUserkod());
        String userkod2 = KeyFormatter.hideHyphenChar(order2.getUserkod());

        log.info(Prefixes.TEST_PREFIX+"[After adding] Orders for order1 size for userkod: "+redisService.getZSetSize(userkod1));
        log.info(Prefixes.TEST_PREFIX+"[After adding] Orders for order2 size for userkod: "+redisService.getZSetSize(userkod2));
        log.info(Prefixes.TEST_PREFIX+"[After adding] Orders for order1 size for region: "+redisService.getZSetSize(topic.getTopic(order1)));
        log.info(Prefixes.TEST_PREFIX+"[After adding] Orders for order2 size for region: "+redisService.getZSetSize(topic.getTopic(order2)));

        redisOrderPublisher.deleteOrder(order1);
        redisOrderPublisher.deleteOrder(order2);

        log.info(Prefixes.TEST_PREFIX+"[After deleting] Orders for order1 size for userkod: "+redisService.getZSetSize(userkod1));
        log.info(Prefixes.TEST_PREFIX+"[After deleting] Orders for order2 size for userkod: "+redisService.getZSetSize(userkod2));
        log.info(Prefixes.TEST_PREFIX+"[After deleting] Orders for order1 size for region: "+redisService.getZSetSize(topic.getTopic(order1)));
        log.info(Prefixes.TEST_PREFIX+"[After deleting] Orders for order2 size for region: "+redisService.getZSetSize(topic.getTopic(order2)));

        Assert.isTrue(redisService.getZSetSize(userkod1) == 0,
                String.format("Пользователь '%s' должен иметь ноль записей заказов!", order1.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(userkod2) == 0,
                String.format("Пользователь '%s' должен иметь ноль записей заказов!", order2.getUserkod()));


        Assert.isTrue(redisService.getZSetSize(topic.getTopic(order2)) == 0,
                String.format("Для темы '%s' должно быть 0 записей!", topic.getTopic(order1)));

        //Темы у order1-4 одинаковые (темы - комбинация названий страны.края.города)
        Assert.isTrue(redisService.getZSetSize(topic.getTopic(order2)) == 0,
                String.format("Для темы '%s' должно быть 0 записей!", topic.getTopic(order2)));

    }
}