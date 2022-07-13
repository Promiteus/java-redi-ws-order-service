package com.romanm.websocket_and_redis.models.responses;

import lombok.Data;
import com.romanm.websocket_and_redis.models.responses.ResponseObjectsData;

@Data
public class ResponseObjectPageableData extends ResponseObjectsData {
    private long page;
    private long pageSize;

    public ResponseObjectPageableData() {
        super();
        this.page = 0;
        this.pageSize = 10;
    }
}
