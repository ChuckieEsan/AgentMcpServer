package com.gov.mock.dto;

public record Response(
        Boolean success,
        String message,
        Object data
) {
    public Response(Boolean success, String message) {
        this(success, message, null);
    }
}
