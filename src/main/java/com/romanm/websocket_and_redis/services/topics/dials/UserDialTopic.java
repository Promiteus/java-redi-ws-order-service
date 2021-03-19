package com.romanm.websocket_and_redis.services.topics.dials;

import com.romanm.websocket_and_redis.constants.Prefixes;
import com.romanm.websocket_and_redis.models.dials.Dial;
import com.romanm.websocket_and_redis.services.topics.Topic;
import com.romanm.websocket_and_redis.utils.KeyFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userDialTopic")
public class UserDialTopic implements Topic<Dial> {

    @Override
    public String getTopic(Dial object) {
        return Prefixes.USER_DIAL_PREFIX+ KeyFormatter.hideHyphenChar(object.getOrder().getUserkod());
    }
}
