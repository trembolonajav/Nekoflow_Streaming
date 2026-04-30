package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminHeroResponse(
    List<String> animeIds,
    String tag,
    String ctaLabel
) {
}
