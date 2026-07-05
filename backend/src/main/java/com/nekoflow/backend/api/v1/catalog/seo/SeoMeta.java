package com.nekoflow.backend.api.v1.catalog.seo;

import java.util.List;

/**
 * Metadados de <head> para uma rota publica. canonicalPath e relativo (ex.:
 * "/anime/frieren"); o base URL e adicionado na renderizacao.
 */
public record SeoMeta(
    String title,
    String description,
    String canonicalPath,
    boolean noindex,
    boolean socialEnabled,
    String ogType,
    String imageUrl,
    String structuredDataKind,
    List<String> alternateNames,
    List<String> genres,
    Integer numberOfEpisodes,
    Integer episodeNumber,
    String seriesName,
    String seriesPath,
    String embedUrl,
    Integer durationSeconds
) {
    public SeoMeta(String title, String description, String canonicalPath, boolean noindex) {
        this(title, description, canonicalPath, noindex, !noindex, "website", SeoMetadataService.DEFAULT_IMAGE_PATH);
    }

    public SeoMeta(
        String title,
        String description,
        String canonicalPath,
        boolean noindex,
        boolean socialEnabled,
        String ogType,
        String imageUrl
    ) {
        this(title, description, canonicalPath, noindex, socialEnabled, ogType, imageUrl,
            socialEnabled ? "generic" : "none", List.of(), List.of(), null, null, null, null, null, null);
    }
}
