package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record HomeResponse(
    HeroBlockResponse hero,
    List<HomeSectionResponse> sections
) {
}
