package com.nekoflow.backend.domain.repository;

import java.time.OffsetDateTime;

/**
 * Projecao enxuta para sitemap de episodios/video. Carrega apenas campos
 * necessarios, sem materializar entidades inteiras.
 */
public interface EpisodeSitemapView {
    String getAnimeSlug();

    String getAnimeTitle();

    String getAnimeSynopsis();

    String getCoverUrl();

    String getBannerUrl();

    Integer getEpisodeNumber();

    String getEpisodeTitle();

    String getSummary();

    String getThumbnailUrl();

    Integer getDurationSeconds();

    OffsetDateTime getPublishedAt();

    String getEmbedUrl();

    String getPlayerUrl();
}
