package com.romanm.websocket_and_redis.models.orders;

public class OrderBuilder {
        private static Order order;
        private static OrderBuilder instance;

        private OrderBuilder() {
            this.order = new Order();
        }

        public static OrderBuilder create(Order od) {
           instance = new OrderBuilder();
           order = od;
           return instance;
        }

        public static OrderBuilder create() {
            instance = new OrderBuilder();
            return instance;
        }

        public static OrderBuilder setOrderId(String id) {
            order.setId(id);
            return instance;
        }

        public static OrderBuilder setOrderName(String name) {
            order.setName(name);
            return instance;
        }

        public OrderBuilder setCode(long code) {
            order.setCode(code);
            return instance;
        }

        public OrderBuilder setUserkod(String userkod) {
            order.setUserkod(userkod);
            return instance;
        }

        public OrderBuilder setCountry(String country) {
            order.setCountry(country);
            return instance;
        }

        public OrderBuilder setRegion(String region) {
            order.setRegion(region);
            return instance;
        }

        public OrderBuilder setLocality(String locality) {
            order.setLocality(locality);
            return instance;
        }

        public OrderBuilder setStatus(Order.STATUS status) {
            order.setStatus(status);
            return instance;
        }

        public static Order build() {
            return order;
        }

}
