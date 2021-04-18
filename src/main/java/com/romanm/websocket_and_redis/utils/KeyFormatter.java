package com.romanm.websocket_and_redis.utils;


import java.util.ArrayList;
import java.util.List;

public class KeyFormatter {
    public static String hideChar(String source, String achar) {
        return source.replace(achar, "");
    }

    public static String hideHyphenChar(String source) {
        return source.replace("-", "");
    }

    //Возвращает список из двух элементов, где содержится начальное и конечное значение страницы. Или пустой список,
    //если параметры страницы неверны.
    public static List<Long> pagePointer(long page, long pageSize) {
        if ((page >= 0) && (pageSize > 0)) {
            return List.of(page*pageSize, page*pageSize+pageSize);
        }
        return new ArrayList<>();
    }
}
