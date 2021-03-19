package com.romanm.websocket_and_redis.utils;

public class ValueChecker {
    public static boolean valueIsEmptyOrNull(Object value) {
        if (value instanceof String) {
            return (value == null) || (((String) value).isEmpty());
        }
        return (value == null);
    }
}
