package com.leyou.common.vo;

import com.leyou.common.enums.ExceptionEnum;
import lombok.Data;


@Data
public class ExceptionResult {

    private int status;

    private String message;

    private long timestamp;

    public ExceptionResult(ExceptionEnum em) {
        this.status = em.value();
        this.message = em.message();
        this.timestamp = System.currentTimeMillis();
    }
}
