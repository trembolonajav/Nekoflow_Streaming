package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryProjection;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.enums.VisibilityStatus;

public interface AnimeRepository extends JpaRepository<AnimeEntity, UUID> {

    @EntityGraph(attributePaths = {"episodes"})
    java.util.List<AnimeEntity> findAllByOrderByTitleDisplayAsc();

    // Sitemap: apenas slug + data, sem carregar a entidade inteira.
    java.util.List<AnimeSitemapView> findByVisibilityOrderBySlugAsc(VisibilityStatus visibility);

    @Query("""
        select a.slug as slug, a.publishedAt as publishedAt
        from AnimeEntity a
        where a.visibility = :visibility
        order by a.slug asc
        """)
    java.util.List<AnimeSitemapView> findSitemapPublished(
        @Param("visibility") VisibilityStatus visibility,
        Pageable pageable
    );

    // Listagem/busca publica: filtro, ordenacao e paginacao NO BANCO, com
    // projecao enxuta e episodeCount agregado (sem N+1, sem load-all).
    @Query("""
        select new com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryProjection(
            a.id, a.slug, a.anilistId, a.titleDisplay, a.titleRomaji, a.synopsis,
            a.coverUrl, a.bannerUrl, a.type, a.status, a.visibility, a.seasonLabel,
            a.year, a.studio, a.averageScore, a.genres,
            (select count(e) from EpisodeEntity e where e.anime = a))
        from AnimeEntity a
        where a.visibility = :visibility
          and (:query = '' or a.searchIndex like :like)
        order by a.titleDisplay asc
        """)
    Slice<AnimeSummaryProjection> searchPublished(
        @Param("visibility") VisibilityStatus visibility,
        @Param("query") String query,
        @Param("like") String like,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"episodes"})
    Optional<AnimeEntity> findBySlugAndVisibility(String slug, VisibilityStatus visibility);

    Optional<AnimeEntity> findBySlugIgnoreCase(String slug);

    Optional<AnimeEntity> findByAnilistId(Long anilistId);
}
