package com.romanm.websocket_and_redis.models.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Data
public class Order implements Serializable {
    private String id;
    private long code;
    private String name;
    private String userkod;
    private String country;
    private String region;
    private String locality;
    private STATUS status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate created;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate modified;

    public Order() {
        this.id = UUID.randomUUID().toString();
        this.userkod = null;
        this.code = new Random().nextLong();
        this.created = LocalDate.now();
        this.modified = LocalDate.now();

        this.region = "";
        this.country = "";
        this.locality = "";

        this.status = STATUS.NEW;
    }

    public Order(String name) {
        this();
        this.name = name;
    }

    public static enum STATUS {
        NEW, COMPLETED, PROCESSING, REJECTED
    }
}
