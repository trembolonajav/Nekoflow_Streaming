package com.nekoflow.backend.api.v1.common.dto;

import java.util.List;

public record ApiPageResponse<T>(
    List<T> items,
    long total,
    int page,
    int size
) {
}
