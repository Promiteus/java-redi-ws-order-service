package com.romanm.websocket_and_redis.services.redis.publishers.dials;

import com.romanm.websocket_and_redis.components.utils.RedisSetStreamFilter;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.events.dials.DialEventPublisher;
import com.romanm.websocket_and_redis.events.orders.OrderEventPublisher;
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
import org.springframework.transaction.annotation.Transactional;

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
    private Topic<Dial> dialTopicExec;
    private RedisSetStreamFilter redisSetStreamFilter;
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    public RedisDialPublisherV1(Topic<Order> topic,
                                @Qualifier("userDialTopic") Topic<Dial> dialTopic,
                                @Qualifier("executorDialTopic") Topic<Dial> dialTopicExec,
                                RedisService redisService,
                                DialEventPublisher dialEventPublisher,
                                DialJsonConverter dialJsonConverter,
                                OrderJsonConverter orderJsonConverter,
                                RedisSetStreamFilter redisSetStreamFilter,
                                OrderEventPublisher orderEventPublisher) {
        this.topic = topic;
        this.redisService = redisService;
        this.dialEventPublisher = dialEventPublisher;
        this.orderEventPublisher = orderEventPublisher;
        this.dialJsonConverter = dialJsonConverter;
        this.orderJsonConverter = orderJsonConverter;
        this.dialTopic = dialTopic;
        this.dialTopicExec = dialTopicExec;
        this.redisSetStreamFilter = redisSetStreamFilter;
    }

    @Override
    public Dial publishDial(Dial dial) {

        this.redisService.getRedisTemplate().execute(new SessionCallback() {
            //@SneakyThrows
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String userkod = KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod());
                String selkod = KeyFormatter.hideHyphenChar(dial.getSelkod());

                Stream orderStream = redisSetStreamFilter.filterRedisSet(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId());

                if (orderStream.toArray().length == 0) {
                    log.info("Can't create dial! There is no such order with orderid: "+dial.getOrder().getId());
                    return null;
                }

                try{
                    //Отслеживать состояние заказов у заказчика с идентификатором userkod
                    redisOperations.watch(userkod);

                    long size1 = redisSetStreamFilter.doubleFilterRedisSetSize(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId(), "selkod");
                    log.info("[Before removing]: redisSetStreamFilter size: "+size1);

                    if (size1 > 0) {
                        //Чтобы Order не оставался для региона и userkod для установленной сделки, то его нужно повторно удалить
                        redisOperations.opsForZSet().remove(userkod, orderJsonConverter.convertObjectToJson(dial.getOrder()));
                        redisOperations.opsForZSet().remove(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
                        return null;
                    };

                    redisOperations.multi();

                    //1. Удалить заказ из множества ZSET(userkod, ORDER{})
                    redisOperations.opsForZSet().remove(userkod, orderJsonConverter.convertObjectToJson(dial.getOrder()));

                    long size2 = redisSetStreamFilter.doubleFilterRedisSetSize(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId(), "selkod");
                    log.info("[After removing]: redisSetStreamFilter size: "+size2);

                    //2. Множество с заказми по региону для исполнителей (ZSET(topic, ORDER{})) также зачищается.
                    log.info("Deleting topic region: "+dial.getOrder().getStatus());
                    redisOperations.opsForZSet().remove(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));

                    //3. Выставить заказу статус "PROCESSING"
                    dial.getOrder().setStatus(Order.STATUS.PROCESSING);

                    //4. Создать упорядоченное множество для исполнителей по сделкам: SET(selkod, DIAL{}, sortValue)
                    redisOperations.opsForZSet().add(selkod, dialJsonConverter.convertObjectToJson(dial), new Date().getTime());

                    //5. Добаваить во множество из п.1. значение сделки: ZSET(userkod, DIAL{})
                    redisOperations.opsForZSet().add(userkod, dialJsonConverter.convertObjectToJson(dial), new Date().getTime());
                    //6. Занести занятый заказ в процессингову карту, связав ее с исполнителем заказа.
                    //Так можно будет понять, занят ли уже заказ другим исполнителем, на тот случай, если уведомление
                    //об этом вовремя не пришло.
                    redisOperations.opsForHash().put(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId(), dial.getSelkod());

                    //7. Отправить уведомление по каналу (orders:country.region.locality) websocket о смене статуса заказа ORDER{} с NEW на PROCESSING
                    //в канал открытых заказов.
                    orderEventPublisher.publishOrderEvent(topic.getTopic(dial.getOrder()), orderJsonConverter.convertObjectToJson(dial.getOrder()));
                    //8. Отправить уведомление по каналу websocket о создании сделки с исполнителем (dials:userkod)
                    dialEventPublisher.publishDialEvent(dialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));

                    redisOperations.exec();
                }catch (Exception e){
                    log.info("It was rollback!!!");
                    redisOperations.discard();
                }

                return null;
            }
        });
        return null;
    }

    @Override
    public Dial deleteDial(Dial dial, boolean isUser) {
        this.redisService.getRedisTemplate().execute(new SessionCallback() {
            @SneakyThrows
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {

                String userkod = KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod());
                String selkod = KeyFormatter.hideHyphenChar(dial.getSelkod());

                redisOperations.watch(selkod);
                redisOperations.watch(userkod);

                redisOperations.multi();
                //1. Удалить сделку из множества для исполнителя
                redisOperations.opsForZSet().remove(selkod, dialJsonConverter.convertObjectToJson(dial));
                //2. Удалить связь исполнителя и заказчика из множества
                redisOperations.opsForZSet().remove(userkod, dialJsonConverter.convertObjectToJson(dial));
                //3. Удалить из карты с именем REDIS_BUSY_ORDERS код заказа и объект сделки
                redisOperations.opsForHash().delete(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId());

                //4. Сменить статус сделки на REJECTED (отклонена) и переместить ее обратно в заказы или не делать ничего
                dial.getOrder().setStatus(Order.STATUS.REJECTED);
                if (!isUser) { //Если исполнитель, преобразовать сделку в заказ и вернуть в список заказов
                    //Если отменил сделку исполнитель, то возвращаем заказ обратно в список с другими невыполненными
                    Order order = dial.getOrder();
                    order.setStatus(Order.STATUS.NEW);
                    //Помещаем/создаем заявку пользователя-заказчика в специальное множество имени userkod
                    redisOperations.opsForZSet().add(userkod, orderJsonConverter.convertObjectToJson(order), new Date().getTime());
                    //Получаем транслированное имя региона и создаем структуру для чтения заказов исполнителями по регионам
                    redisOperations.opsForZSet().add(topic.getTopic(order), orderJsonConverter.convertObjectToJson(order), new Date().getTime());
                    //Отправить в канал websocket уведомление о смене статуса заказа (Новый заказ)
                    orderEventPublisher.publishOrderEvent(topic.getTopic(order), orderJsonConverter.convertObjectToJson(order));
                    //Отправить уведомление заказчику по каналу websocket об удалении сделки с исполнителем (user_dials:userkod)
                    dialEventPublisher.publishDialEvent(dialTopic.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));
                } else { //Если заказчик
                    //Отправить уведомление исполнителю по каналу websocket об удалении сделки с исполнителем (user_dials:userkod)
                    dialEventPublisher.publishDialEvent(dialTopicExec.getTopic(dial), dialJsonConverter.convertObjectToJson(dial));
                }

                redisOperations.exec();

                return null;
            }
        });
        return dial;
    }
}
