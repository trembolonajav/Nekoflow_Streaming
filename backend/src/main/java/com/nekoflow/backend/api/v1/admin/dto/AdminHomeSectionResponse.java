package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminHomeSectionResponse(
    String code,
    String title,
    String mode,
    boolean active,
    Integer sortOrder,
    List<String> manualItemIds
) {
}
