package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record HomeSectionResponse(
    String code,
    String title,
    String mode,
    boolean active,
    List<HomeSectionItemResponse> items
) {
}
