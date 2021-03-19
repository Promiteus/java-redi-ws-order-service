package com.romanm.websocket_and_redis.utils;

import org.springframework.stereotype.Component;

public class KeyFormatter {
    public static String hideChar(String source, String achar) {
        return source.replace(achar, "");
    }

    public static String hideHyphenChar(String source) {
        return source.replace("-", "");
    }
}
