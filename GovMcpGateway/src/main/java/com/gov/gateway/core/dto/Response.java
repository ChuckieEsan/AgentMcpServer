package com.gov.gateway.core.dto;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Response(
        Boolean success,
        String message,
        Object data
) {
    Response(Boolean success, String message) {
        this(success, message, null);
    }

}
