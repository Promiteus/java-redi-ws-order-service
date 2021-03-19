package com.romanm.websocket_and_redis.models.responses;

import lombok.Data;

@Data
public class ResponseObjectPageableData extends ResponseObjectData {
    private long page;
    private long pageSize;

    public ResponseObjectPageableData() {
        super();
        this.page = 0;
        this.pageSize = 10;
    }
}
