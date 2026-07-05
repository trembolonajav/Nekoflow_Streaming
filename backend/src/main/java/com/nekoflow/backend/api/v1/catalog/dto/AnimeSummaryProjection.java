package com.nekoflow.backend.api.v1.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.VisibilityStatus;

/**
 * Projecao para o card de anime na listagem/busca. Traz apenas os campos
 * necessarios para o card + a contagem de episodios via subquery agregada,
 * evitando materializar a entidade inteira e o N+1 de anime.getEpisodes().size().
 */
public record AnimeSummaryProjection(
    UUID id,
    String slug,
    Long anilistId,
    String titleDisplay,
    String titleRomaji,
    String synopsis,
    String coverUrl,
    String bannerUrl,
    AnimeType type,
    AnimeStatus status,
    VisibilityStatus visibility,
    String seasonLabel,
    Integer year,
    String studio,
    BigDecimal averageScore,
    String genresRaw,
    Long episodeCount
) {
}
