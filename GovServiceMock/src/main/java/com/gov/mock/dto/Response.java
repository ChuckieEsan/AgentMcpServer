package com.gov.mock.dto;

import java.io.Serializable;

public record Response(
        Boolean success,
        String message,
        Object data
) implements Serializable {
    public Response(Boolean success, String message) {
        this(success, message, null);
    }
}
