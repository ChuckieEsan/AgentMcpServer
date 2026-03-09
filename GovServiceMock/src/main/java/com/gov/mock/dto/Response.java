package com.gov.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String message;
    private Object data;

    public Response(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }
}