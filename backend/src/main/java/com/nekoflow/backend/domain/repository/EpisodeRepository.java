package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;

public interface EpisodeRepository extends JpaRepository<EpisodeEntity, UUID> {

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    java.util.List<EpisodeEntity> findAllByOrderByPublishedAtDescNumberDesc();

    // Home "recentes": episodios publicados de animes publicados, ordenados e
    // LIMITADOS no banco (Pageable), com o anime carregado via join fetch.
    // Sem carregar todos os episodios so para pegar os N mais recentes.
    @Query("""
        select e from EpisodeEntity e
        join fetch e.anime a
        where e.status = :status and a.visibility = :visibility
        order by e.publishedAt desc, e.number desc
        """)
    java.util.List<EpisodeEntity> findRecentPublished(
        @Param("status") EpisodeStatus status,
        @Param("visibility") VisibilityStatus visibility,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    Optional<EpisodeEntity> findByAnimeSlugAndNumberAndStatus(String animeSlug, Integer number, EpisodeStatus status);

    boolean existsById(UUID id);

    boolean existsByAnimeIdAndNumber(UUID animeId, Integer number);

    boolean existsByAnimeIdAndNumberAndIdNot(UUID animeId, Integer number, UUID id);

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    Optional<EpisodeEntity> findByAnimeIdAndNumber(UUID animeId, Integer number);

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    java.util.List<EpisodeEntity> findAllByStatusInOrderByScheduledForAscPublishedAtAscNumberAsc(java.util.Collection<EpisodeStatus> statuses);

    @Query("""
        select
            a.slug as animeSlug,
            a.titleDisplay as animeTitle,
            a.synopsis as animeSynopsis,
            a.coverUrl as coverUrl,
            a.bannerUrl as bannerUrl,
            e.number as episodeNumber,
            e.title as episodeTitle,
            e.summary as summary,
            e.thumbnailUrl as thumbnailUrl,
            e.durationSeconds as durationSeconds,
            e.publishedAt as publishedAt,
            v.embedUrl as embedUrl,
            v.playerUrl as playerUrl
        from EpisodeEntity e
        join e.anime a
        join e.videoSources v
        where e.status = :status
          and a.visibility = :visibility
          and v.status = :videoStatus
          and v.isDefault = true
          and (v.embedUrl is not null or v.playerUrl is not null)
        order by a.slug asc, e.number asc
        """)
    java.util.List<EpisodeSitemapView> findSitemapPublishedEpisodes(
        @Param("status") EpisodeStatus status,
        @Param("visibility") VisibilityStatus visibility,
        @Param("videoStatus") VideoSourceStatus videoStatus,
        Pageable pageable
    );
}
