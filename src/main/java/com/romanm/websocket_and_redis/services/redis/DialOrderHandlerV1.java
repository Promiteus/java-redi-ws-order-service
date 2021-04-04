package com.romanm.websocket_and_redis.services.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.events.dials.DialEventPublisher;
import com.romanm.websocket_and_redis.events.orders.OrderEventPublisher;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.repositories.RedisRepository;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.services.topics.dials.ExecutorDialTopic;
import com.romanm.websocket_and_redis.utils.DialJsonConverter;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;


@Service("dialOrderHandlerV1")
@Slf4j
public class DialOrderHandlerV1 implements DialOrderHandler {

    private RedisService redisService;
    private DialEventPublisher dialEventPublisher;
    private DialJsonConverter dialJsonConverter;
    private OrderJsonConverter orderJsonConverter;
    private OrderEventPublisher orderEventPublisher;
    private Topic<Dial> dialTopic;
    private Topic<Order> topic;
    private Topic<Dial> executorDialTopic;

    @Autowired
    public DialOrderHandlerV1(RedisService redisService,
                              DialEventPublisher dialEventPublisher,
                              DialJsonConverter dialJsonConverter,
                              OrderJsonConverter orderJsonConverter,
                              OrderEventPublisher orderEventPublisher,
                              @Qualifier("userDialTopic") Topic<Dial> dialTopic,
                              Topic<Order> topic,
                              Topic<Dial> executorDialTopic) {
        this.dialEventPublisher = dialEventPublisher;
        this.dialJsonConverter = dialJsonConverter;
        this.orderEventPublisher = orderEventPublisher;
        this.redisService = redisService;
        this.orderJsonConverter = orderJsonConverter;
        this.dialTopic = dialTopic;
        this.topic = topic;
        this.executorDialTopic = executorDialTopic;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("postConstruct!");
    }

    @Override
    public boolean isSuccessDial(Dial dial) {
        Object res = redisService.getHashMapItem(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId());
        if (res != null) {
            return res.toString().contains(dial.getSelkod());
        }
        return false;
    }

    @Override
    public void notifyDialOrderStatus(Dial dial, boolean success) throws JsonProcessingException {
        if (success)  {
            dial.getOrder().setStatus(Order.STATUS.PROCESSING);
            //Отправить уведомление по каналу (orders:country.region.locality) websocket о смене статуса заказа ORDER{} с NEW на PROCESSING
            //в канал открытых заказов.
            orderEventPublisher.publishOrderEvent(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
            //Отправить уведомление по каналу websocket о создании сделки с исполнителем (dials:userkod)
            dialEventPublisher.publishDialEvent(dialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));
        }
    }

    @Override
    public void notifyDialStatus(Dial dial, boolean toExecutor) throws JsonProcessingException {
        if (!toExecutor) {
            dialEventPublisher.publishDialEvent(dialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));
        } else {
            dialEventPublisher.publishDialEvent(executorDialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));
        }
    }

    @Override
    public void notifyOrderStatus(Order order) throws JsonProcessingException {
        orderEventPublisher.publishOrderEvent(topic.getTopic(order), orderJsonConverter.convertObjectToJson(order));
    }

    @Override
    public void deleteOrderOps(RedisOperations redisOps, Dial dial) throws JsonProcessingException {
        dial.getOrder().setStatus(Order.STATUS.NEW);
        //Удалить заказ из множества ZSET(userkod, ORDER{})
        redisOps.opsForZSet().remove(KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
        //Множество с заказми по региону для исполнителей (ZSET(topic, ORDER{})) также зачищается.
        log.info("Deleting topic region: "+dial.getOrder().getStatus());
        redisOps.opsForZSet().remove(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
    }

    @Override
    public void addOrder(RedisOperations redisOps, Order order) throws JsonProcessingException {
        //Помещаем заявку заказчика в специальное множество имени userkod
        redisOps.opsForZSet().add(KeyFormatter.hideHyphenChar(order.getUserkod()), orderJsonConverter.convertObjectToJson(order), new Date().getTime());
        //Получаем транслированное имя региона и создаем структуру для чтения заказов исполнителями по регионам
        redisOps.opsForZSet().add(topic.getTopic(order), orderJsonConverter.convertObjectToJson(order), new Date().getTime());

    }

    @Override
    public void removeDial(RedisOperations redisOps, Dial dial) throws JsonProcessingException {
        //1. Удалить сделку из множества для исполнителя
        redisOps.opsForZSet().remove(KeyFormatter.hideHyphenChar(dial.getSelkod()), dialJsonConverter.convertObjectToJson(dial));
        //2. Удалить связь исполнителя и заказчика из множества
        redisOps.opsForZSet().remove(KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod()), dialJsonConverter.convertObjectToJson(dial));
        //3. Удалить из карты с именем REDIS_BUSY_ORDERS код заказа и объект сделки
        redisOps.opsForHash().delete(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId());
    }
}
