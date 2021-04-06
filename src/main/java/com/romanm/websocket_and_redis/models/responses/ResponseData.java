package com.romanm.websocket_and_redis.models.responses;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResponseData {
    private int status;
    private Object object;
    private String error;
    private LocalDateTime timestamp;

    public ResponseData() {
        this.status = 500;
        this.object = null;
        this.error = "";
        this.timestamp = LocalDateTime.now();
    }

    public ResponseData(Object data, int status) {
        this();
        this.object = data;
        this.status = status;
    }

    public ResponseData(Object data, int status, String error) {
        this();
        this.object = data;
        this.error = error;
        this.status = status;
    }
}
