package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminHomeResponse(
    AdminHeroResponse hero,
    List<AdminHomeSectionResponse> sections
) {
}
