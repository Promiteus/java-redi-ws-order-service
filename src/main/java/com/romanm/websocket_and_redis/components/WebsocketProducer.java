package com.romanm.websocket_and_redis.components;

import com.romanm.websocket_and_redis.configs.BrokerConfiguration;
import com.romanm.websocket_and_redis.utils.ValueChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebsocketProducer {

    private static String SUB_ORDERS_PATH = "%s/%s";
    private static String SUB_DIALS_PATH = "%s/%s";

    private SimpMessagingTemplate simpMessagingTemplate;
    private BrokerConfiguration brokerConfiguration;


    @Autowired
    public WebsocketProducer(SimpMessagingTemplate simpMessagingTemplate,
                             BrokerConfiguration brokerConfiguration) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.brokerConfiguration = brokerConfiguration;
    }

    //Уведомить через websocket о заказе или его новом статусе
    public void sendOrderDataToTopic(String channel, String data) {
        if ((!ValueChecker.valueIsEmptyOrNull(data)) && (!ValueChecker.valueIsEmptyOrNull(channel))) {
            log.info(String.format("sendOrderDataToTopic execute for channel %s/%s", brokerConfiguration.getOrderBroker(), channel));
            simpMessagingTemplate.convertAndSend(String.format(SUB_ORDERS_PATH, brokerConfiguration.getOrderBroker(), channel), data);
        } else {
            log.error("[sendOrderDataToTopic] Channel or data is empty!");
        }
    }

    //Уведомить о сделке или о ее новом статусе
    public void sendDialDataToTopic(String channel, String data) {
        if ((!ValueChecker.valueIsEmptyOrNull(data)) && (!ValueChecker.valueIsEmptyOrNull(channel))) {
            log.info(String.format("sendDialDataToTopic execute for channel %s/%s", brokerConfiguration.getDialBroker(), channel));
            simpMessagingTemplate.convertAndSend(String.format(SUB_DIALS_PATH, brokerConfiguration.getDialBroker(), channel), data);
        } else {
            log.error("[sendDialDataToTopic] Channel or data is empty!");
        }
    }
}
