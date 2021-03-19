package com.romanm.websocket_and_redis.services.redis.publishers.dials;

import com.romanm.websocket_and_redis.components.utils.RedisSetStreamFilter;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.events.dials.DialEventPublisher;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.services.redis.RedisService;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import com.romanm.websocket_and_redis.utils.DialJsonConverter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Stream;

@Slf4j
@Service
public class RedisDialPublisherV1 implements RedisDialPublisher {
    private Topic<Order> topic;
    private RedisService redisService;
    private DialEventPublisher dialEventPublisher;
    private DialJsonConverter dialJsonConverter;
    private OrderJsonConverter orderJsonConverter;
    private Topic<Dial> dialTopic;
    private RedisSetStreamFilter redisSetStreamFilter;

    @Autowired
    public RedisDialPublisherV1(Topic<Order> topic,
                                @Qualifier("userDialTopic") Topic<Dial> dialTopic,
                                RedisService redisService,
                                DialEventPublisher dialEventPublisher,
                                DialJsonConverter dialJsonConverter,
                                OrderJsonConverter orderJsonConverter,
                                RedisSetStreamFilter redisSetStreamFilter) {
        this.topic = topic;
        this.redisService = redisService;
        this.dialEventPublisher = dialEventPublisher;
        this.dialJsonConverter = dialJsonConverter;
        this.orderJsonConverter = orderJsonConverter;
        this.dialTopic = dialTopic;
        this.redisSetStreamFilter = redisSetStreamFilter;
    }

    @Override
    public Dial publishDial(Dial dial) {

        this.redisService.getRedisTemplate().execute(new SessionCallback() {
            @SneakyThrows
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String userkod = KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod());
                String selkod = KeyFormatter.hideHyphenChar(dial.getSelkod());

                Stream orderStream = redisSetStreamFilter.filterRedisSet(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId());

                if (orderStream.toArray().length == 0) {
                    log.info("Can't create dial! There is no such order with orderid: "+dial.getOrder().getId());
                    return null;
                }

                redisOperations.multi();

                //1. Удалить заказ из множества ZSET(userkod, ORDER{})
                redisOperations.opsForZSet().remove(userkod, orderJsonConverter.convertObjectToJson(dial.getOrder()));
                //2. Множество с заказми по региону для исполнителей (ZSET(topic, ORDER{})) также зачищается.
                redisOperations.opsForZSet().remove(topic.getTopic(dial.getOrder()), dial.getOrder());

                //3. Выставить заказу статус "PROCESSING"
                dial.getOrder().setStatus(Order.STATUS.PROCESSING);

                //4. Создать упорядоченное множество для исполнителей по сделкам: SET(selkod, DIAL{}, sortValue)
                redisOperations.opsForZSet().add(selkod, dial, new Date().getTime());
                //5. Добаваить во множество из п.1. значение сделки: ZSET(userkod, DIAL{})
                redisOperations.opsForZSet().add(userkod, dialJsonConverter.convertObjectToJson(dial), new Date().getTime());
                //6. Занести занятый заказ в процессингову карту, связав ее с исполнителем заказа.
                //Так можно будет понять, занят ли уже заказ другим исполнителем, на тот случай, если уведомление
                //об этом вовремя не пришло.
                redisOperations.opsForHash().put(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId(), dial.getSelkod());

                //7. Отправить уведомление по каналу (orders:country.region.locality) websocket о смене статуса заказа ORDER{} с NEW на PROCESSING
                //в канал открытых заказов.
                dialEventPublisher.publishDialEvent(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
                //8. Отправить уведомление по каналу websocket о создании сделки с исполнителем (dials:userkod)
                dialEventPublisher.publishDialEvent(dialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));

                redisOperations.exec();

                return null;
            }
        });
        return null;
    }
}
