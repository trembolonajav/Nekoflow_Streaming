package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record HeroBlockResponse(
    String tag,
    String ctaLabel,
    List<HomeSectionItemResponse> items
) {
}
