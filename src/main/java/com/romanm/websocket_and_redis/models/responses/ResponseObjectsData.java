package com.romanm.websocket_and_redis.models.responses;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResponseObjectsData {
    private int status;
    private List<Object> objects;
    private String error;
    private LocalDateTime timestamp;

    public ResponseObjectsData() {
        this.status = 500;
        this.objects = null;
        this.error = "";
        this.timestamp = LocalDateTime.now();
    }

    public ResponseObjectsData(List<Object> data, int status) {
        this();
        this.objects = data;
        this.status = status;
    }

    public ResponseObjectsData(List<Object> data, int status, String error) {
        this();
        this.objects = data;
        this.error = error;
        this.status = status;
    }
}
