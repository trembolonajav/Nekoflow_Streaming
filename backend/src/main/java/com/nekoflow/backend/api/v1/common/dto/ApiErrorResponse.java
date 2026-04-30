package com.nekoflow.backend.api.v1.common.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    String message,
    int status,
    String path,
    OffsetDateTime timestamp
) {
}
