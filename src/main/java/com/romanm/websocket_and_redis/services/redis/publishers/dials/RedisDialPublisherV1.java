package com.romanm.websocket_and_redis.services.redis.publishers.dials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.components.utils.RedisSetStreamFilter;
import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.events.dials.DialEventPublisher;
import com.romanm.websocket_and_redis.events.orders.OrderEventPublisher;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.services.redis.DialOrderHandler;
import com.romanm.websocket_and_redis.services.redis.RedisService;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import com.romanm.websocket_and_redis.utils.DialJsonConverter;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
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
    private RedisService redisService;
    private DialJsonConverter dialJsonConverter;
    private RedisSetStreamFilter redisSetStreamFilter;
    private DialOrderHandler dialOrderHandler;


    @Autowired
    public RedisDialPublisherV1(Topic<Order> topic,
                                @Qualifier("redisServiceV1") RedisService redisService,
                                DialJsonConverter dialJsonConverter,
                                RedisSetStreamFilter redisSetStreamFilter,
                                DialOrderHandler dialOrderHandler) {
        this.redisService = redisService;
        this.dialJsonConverter = dialJsonConverter;
        this.redisSetStreamFilter = redisSetStreamFilter;
        this.dialOrderHandler = dialOrderHandler;
    }

    @Override
    public Dial publishDial(Dial dial) {

        this.redisService.getRedisTemplate().execute(new SessionCallback() {
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
                    long size = findRecordInSet(userkod, "selkod", dial, redisOperations);//redisSetStreamFilter.doubleFilterRedisSetSize(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId(), "selkod");
                    if (size > 0) {
                        log.info("[Before removing]: redisSetStreamFilter size - this order has dial: "+size);
                        return null;
                    };

                    //Отслеживать состояние заказов у заказчика с идентификатором userkod
                    redisOperations.watch(userkod);

                    redisOperations.multi();

                      //1. Выставить заказу статус "PROCESSING"
                      dial.getOrder().setStatus(Order.STATUS.PROCESSING);
                      //2. Создать упорядоченное множество для исполнителей по сделкам: SET(selkod, DIAL{}, sortValue)
                      redisOperations.opsForZSet().add(selkod, dialJsonConverter.convertObjectToJson(dial), new Date().getTime());
                      //3. Добаваить во множество значение сделки: ZSET(userkod, DIAL{})
                      redisOperations.opsForZSet().add(userkod, dialJsonConverter.convertObjectToJson(dial), new Date().getTime());
                      //4. Занести занятый заказ в процессингову карту, связав ее с исполнителем заказа.
                      //Так можно будет понять, занят ли уже заказ другим исполнителем, на тот случай, если уведомление
                      //об этом вовремя не пришло.
                      redisOperations.opsForHash().put(Prefixes.REDIS_BUSY_ORDERS, dial.getOrder().getId(), dial.getSelkod());
                      //Удалить заказ из списка новых заказов
                      dialOrderHandler.deleteOrderOps(redisOperations, dial);

                    redisOperations.exec();

                    dialOrderHandler.notifyDialOrderStatus(dial, dialOrderHandler.isSuccessDial(dial));
                }catch (Exception e){
                    log.info("[publishDial] It was rollback!!!");
                    redisOperations.discard();
                }

                return dial;
            }
        });
       return null;
    }

    private long findRecordInSet(String key1, String key2, Dial dial, RedisOperations redisOperations) {
        return redisSetStreamFilter.doubleFilterRedisSetSize(redisOperations.opsForZSet().range(key1, 0, -1), dial.getOrder().getId(), key2);
    }


    @Override
    public Dial deleteDial(Dial dial, boolean isUser) {
        this.redisService.getRedisTemplate().execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {

                String userkod = KeyFormatter.hideHyphenChar(dial.getOrder().getUserkod());
                String selkod = KeyFormatter.hideHyphenChar(dial.getSelkod());
                try {
                    long size = findRecordInSet(userkod, "selkod", dial, redisOperations);//redisSetStreamFilter.doubleFilterRedisSetSize(redisOperations.opsForZSet().range(userkod, 0, -1), dial.getOrder().getId(), "selkod");
                    if (size == 0) {
                        log.info("There are no any dials for userkod: "+userkod);
                        return null;
                    };

                    redisOperations.watch(selkod);
                    redisOperations.watch(userkod);

                    redisOperations.multi();
                      dialOrderHandler.removeDial(redisOperations, dial);
                      //Сменить статус сделки на REJECTED (отклонена) и переместить ее обратно в заказы или не делать ничего
                      dial.getOrder().setStatus(Order.STATUS.REJECTED);
                      if (!isUser) { //Если исполнитель, преобразовать сделку в заказ и вернуть в список заказов
                          //Если отменил сделку исполнитель, то возвращаем заказ обратно в список с другими невыполненными
                          Order order = dial.getOrder();
                          order.setStatus(Order.STATUS.NEW);
                          dialOrderHandler.addOrder(redisOperations, order);
                          //Отправить в канал websocket уведомление о смене статуса заказа (Новый заказ)
                          dialOrderHandler.notifyOrderStatus(order);
                          //Отправить уведомление заказчику по каналу websocket об удалении сделки с исполнителем (user_dials:userkod)
                          dialOrderHandler.notifyDialStatus(dial, false);
                      } else { //Если заказчик
                          //Отправить уведомление исполнителю по каналу websocket об удалении сделки с исполнителем (user_dials:userkod)
                          dialOrderHandler.notifyDialStatus(dial, true);
                      }

                    redisOperations.exec();
                } catch (Exception e){
                   log.info("[deleteDial] It was rollback!!!");
                   redisOperations.discard();
                }

                return null;
            }
        });
        return dial;
    }



}
