package com.romanm.websocket_and_redis.constants;

public class Prefixes {
    public static final String ORDER_PREFIX = "orders:";
    public static final String USER_DIAL_PREFIX = "user_dials:";
    public static final String EXECUTOR_DIAL_PREFIX = "exec_dials:";

    public static final String API_ID = "id";
    public static final String API_SLASH_ID = "/{"+API_ID+"}";
    public static final String API_USER = "/user";
    public static final String API_EXECUTOR = "/executor";
    public static final String API_BASE_PATH = "/api";
    public static final String API_DIAL_PATH = "/dial";
    public static final String API_ORDER_PATH = "/order";
    public static final String API_ORDER_DELETE_PATH = API_ORDER_PATH+API_SLASH_ID+API_USER;
    public static final String API_DIAL_DELETE_BY_USER_PATH = API_DIAL_PATH+API_SLASH_ID+API_USER;
    public static final String API_DIAL_DELETE_BY_EXECUTOR_PATH = API_DIAL_PATH+API_SLASH_ID+API_EXECUTOR;


    public static final String REDIS_BUSY_ORDERS = "processings";

    public static final String TEST_PREFIX = ">>> Test Info >>> ";
}
