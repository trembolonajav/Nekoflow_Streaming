package com.nekoflow.backend.domain.repository;

import java.time.OffsetDateTime;

/**
 * Projecao enxuta para o sitemap: carrega apenas slug e data, sem materializar
 * a entidade Anime inteira nem seus episodios.
 */
public interface AnimeSitemapView {
    String getSlug();

    OffsetDateTime getPublishedAt();
}
