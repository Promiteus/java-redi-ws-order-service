package com.romanm.websocket_and_redis.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.romanm.websocket_and_redis.components.WebsocketProducer;
import com.romanm.websocket_and_redis.configs.BrokerConfiguration;
import com.romanm.websocket_and_redis.models.orders.Order;
import com.romanm.websocket_and_redis.models.orders.OrderBuilder;
import com.romanm.websocket_and_redis.models.responses.ResponseObjectData;
import com.romanm.websocket_and_redis.utils.OrderJsonConverter;
import com.romanm.websocket_and_redis.utils.ValueChecker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderJsonConverter orderJsonConverter;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private WebsocketProducer websocketProducer;
    @Autowired
    private BrokerConfiguration brokerConfiguration;


    private static final String URL_POST_ORDER = "/api/order";
    private String URL = "ws://localhost:8081/stomp";

    @Test
    public void postOrderMvcTest() throws Exception {
        Order order = OrderBuilder
                .create()
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск").setUserkod(UUID.randomUUID().toString()).build();

        this.mockMvc
                .perform(post(URL_POST_ORDER).contentType(MediaType.APPLICATION_JSON)
                        .content(orderJsonConverter.convertObjectToJson(order))
                ).andExpect(status().isOk());


    }

    @Test
    public void postOrderRestTempTest() {
        Order order = OrderBuilder
                .create()
                .setCode(1245)
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск").setUserkod(UUID.randomUUID().toString()).build();

        ResponseObjectData response = this.testRestTemplate.postForObject(URL_POST_ORDER, order, ResponseObjectData.class);

        Assert.isTrue(response.getObjects() != null, "Ответ не содержит поле data!");
    }

    private List<Transport> createTransportClient() {
        return List.of(new WebSocketTransport(new StandardWebSocketClient()));
    }

    @Test
    public void stompSubscriptionTest() throws InterruptedException, ExecutionException, TimeoutException, JsonProcessingException {
        WebSocketStompClient webSocketStompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        webSocketStompClient.setMessageConverter(new MappingJackson2MessageConverter());


        System.out.println("URL: "+URL);
        StompSession stompSession = webSocketStompClient.connect(URL, new StompSessionHandlerAdapter() {
        }).get(1, TimeUnit.SECONDS);

        stompSession.subscribe(brokerConfiguration.getBroker()+"/rossiya", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders stompHeaders) {
                return null;
            }

            @Override
            public void handleFrame(StompHeaders stompHeaders, Object o) {
                if (!ValueChecker.valueIsEmptyOrNull(o)) {
                    log.info("---- handleFrame data: {}", o);
                } else {
                    log.error("---- handleFrame has empty data!");
                }
            }
        });

        Order order = OrderBuilder
                .create()
                .setCode(1245)
                .setUserkod(UUID.randomUUID().toString())
                .setCountry("Россия")
                .setRegion("Хабаровский край")
                .setOrderName("Заказ 101")
                .setLocality("Хабаровск").setUserkod(UUID.randomUUID().toString()).build();

        stompSession.send(brokerConfiguration.getBroker()+"/rossiya", order);

        this.websocketProducer.sendOrderDataToTopic("rossiya", orderJsonConverter.convertObjectToJson(order));

    }
}