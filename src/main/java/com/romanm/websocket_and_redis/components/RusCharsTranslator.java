package com.romanm.websocket_and_redis.components;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RusCharsTranslator {
    private String alpha = new String("абвгдеёжзиыйклмнопрстуфхцчшщьэюя ");
    private String[] _alpha = {"a","b","v","g","d","e","yo","g","z","i","y","i",
            "k","l","m","n","o","p","r","s","t","u",
            "f","kh","tz","ch","sh","sh","'","e","yu","ya","_"};

    public String translate(@NonNull String rus) {
        List<String> res = rus.toLowerCase().chars()
                .map(code -> (alpha.indexOf(String.valueOf((char)code))))
                .mapToObj(value -> (new String(Arrays.asList(_alpha).get(value)))).collect(Collectors.toList());
        return String.join("", res);
    }
}
