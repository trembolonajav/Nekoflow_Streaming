package com.nekoflow.backend.api.v1.catalog;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeEpisodeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryProjection;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HeroBlockResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeSectionItemResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeSectionResponse;
import com.nekoflow.backend.api.v1.catalog.dto.WatchPlayerResponse;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.EpisodeVideoSourceEntity;
import com.nekoflow.backend.domain.entity.HeroConfigEntity;
import com.nekoflow.backend.domain.entity.HomeSectionEntity;
import com.nekoflow.backend.domain.entity.HomeSectionItemEntity;
import com.nekoflow.backend.domain.SearchText;
import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.HomeSectionMode;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.HeroConfigRepository;
import com.nekoflow.backend.domain.repository.HomeSectionRepository;

@Service
public class CatalogQueryService {
    private static final int RECENT_SECTION_LIMIT = 60;
    private static final int MAX_QUERY_LENGTH = 100;

    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final HomeSectionRepository homeSectionRepository;
    private final HeroConfigRepository heroConfigRepository;

    public CatalogQueryService(
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        HomeSectionRepository homeSectionRepository,
        HeroConfigRepository heroConfigRepository
    ) {
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.homeSectionRepository = homeSectionRepository;
        this.heroConfigRepository = heroConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<AnimeSummaryResponse> listPublishedAnimes(String query, int page, int size) {
        // Cap no tamanho da query (evita query gigante) + normalizacao para bater
        // com a coluna search_index. Filtro/ordenacao/paginacao acontecem no banco.
        String capped = query != null && query.length() > MAX_QUERY_LENGTH
            ? query.substring(0, MAX_QUERY_LENGTH)
            : query;
        String normalized = SearchText.normalize(capped);
        int limit = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        String like = "%" + normalized + "%";

        return animeRepository.searchPublished(
                VisibilityStatus.PUBLISHED, normalized, like, PageRequest.of(safePage, limit))
            .getContent().stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AnimeDetailResponse> findPublishedAnimeBySlug(String slug) {
        return animeRepository.findBySlugAndVisibility(slug, VisibilityStatus.PUBLISHED)
            .map(this::toDetail);
    }

    @Transactional(readOnly = true)
    public Optional<WatchPlayerResponse> findPublishedEpisodePlayer(String slug, Integer episodeNumber) {
        return episodeRepository.findByAnimeSlugAndNumberAndStatus(slug, episodeNumber, EpisodeStatus.PUBLISHED)
            .map(this::toWatchPlayer);
    }

    @Transactional(readOnly = true)
    public HomeResponse getHome() {
        List<HeroConfigEntity> heroItems = heroConfigRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc();
        HeroBlockResponse hero = new HeroBlockResponse(
            heroItems.isEmpty() ? "Destaque editorial" : defaultString(heroItems.get(0).getHeadline(), "Destaque editorial"),
            heroItems.isEmpty() ? "Assistir agora" : defaultString(heroItems.get(0).getCtaLabel(), "Assistir agora"),
            heroItems.stream()
                .map(this::toHeroItem)
                .filter(java.util.Objects::nonNull)
                .toList()
        );

        List<HomeSectionResponse> sections = homeSectionRepository.findByActiveTrueOrderBySortOrderAsc().stream()
            .filter(section -> !"hero".equals(section.getCode()))
            .map(this::toSection)
            .toList();

        return new HomeResponse(hero, sections);
    }

    private AnimeSummaryResponse toSummary(AnimeSummaryProjection anime) {
        return new AnimeSummaryResponse(
            anime.id().toString(),
            anime.slug(),
            anime.anilistId(),
            anime.titleDisplay(),
            anime.titleRomaji(),
            anime.synopsis(),
            anime.coverUrl(),
            anime.bannerUrl(),
            anime.type().name(),
            anime.status().name(),
            anime.visibility().name(),
            anime.seasonLabel(),
            anime.year(),
            anime.studio(),
            anime.averageScore() != null ? anime.averageScore().doubleValue() : null,
            displayGenres(parseGenres(anime.genresRaw()), anime.type(), anime.status()),
            anime.episodeCount() != null ? anime.episodeCount().intValue() : 0
        );
    }

    private AnimeDetailResponse toDetail(AnimeEntity anime) {
        List<AnimeEpisodeSummaryResponse> episodes = anime.getEpisodes().stream()
            .filter(episode -> episode.getStatus() == EpisodeStatus.PUBLISHED)
            .sorted(Comparator.comparing(EpisodeEntity::getNumber))
            .map(this::toEpisodeSummary)
            .toList();

        return new AnimeDetailResponse(
            anime.getId().toString(),
            anime.getSlug(),
            anime.getAnilistId(),
            anime.getTitleDisplay(),
            anime.getTitleRomaji(),
            anime.getTitleNative(),
            anime.getTitleEnglish(),
            anime.getSynopsis(),
            anime.getType().name(),
            anime.getStatus().name(),
            anime.getVisibility().name(),
            anime.getSeasonLabel(),
            anime.getYear(),
            anime.getCoverUrl(),
            anime.getBannerUrl(),
            anime.getStudio(),
            displayGenres(anime),
            episodes
        );
    }

    private AnimeEpisodeSummaryResponse toEpisodeSummary(EpisodeEntity episode) {
        return new AnimeEpisodeSummaryResponse(
            episode.getId().toString(),
            episode.getNumber(),
            episode.getTitle(),
            episode.getStatus().name(),
            episode.getThumbnailUrl(),
            episode.getDurationSeconds()
        );
    }

    private WatchPlayerResponse toWatchPlayer(EpisodeEntity episode) {
        EpisodeVideoSourceEntity source = episode.getVideoSources().stream()
            .filter(video -> video.getStatus().name().equals("ACTIVE"))
            .sorted(Comparator.comparing(EpisodeVideoSourceEntity::isDefault).reversed())
            .findFirst()
            .orElse(null);

        return new WatchPlayerResponse(
            episode.getAnime().getSlug(),
            episode.getAnime().getTitleDisplay(),
            episode.getId().toString(),
            episode.getNumber(),
            episode.getTitle(),
            episode.getSummary(),
            episode.getThumbnailUrl(),
            source != null ? source.getProvider().name() : null,
            source != null ? source.getEmbedUrl() : null,
            source != null ? source.getPlayerUrl() : null,
            episode.getDurationSeconds()
        );
    }

    private HomeSectionResponse toSection(HomeSectionEntity section) {
        List<HomeSectionItemResponse> items = resolveSectionItems(section);

        return new HomeSectionResponse(
            section.getCode(),
            section.getTitle(),
            section.getMode().name(),
            section.isActive(),
            items
        );
    }

    private List<HomeSectionItemResponse> resolveSectionItems(HomeSectionEntity section) {
        List<HomeSectionItemResponse> manualItems = section.getItems().stream()
            .sorted(Comparator.comparing(HomeSectionItemEntity::getSortOrder))
            .map(this::toSectionItem)
            .toList();

        if (!"recent".equals(section.getCode()) || section.getMode() == HomeSectionMode.MANUAL) {
            return manualItems;
        }

        List<HomeSectionItemResponse> automaticItems = episodeRepository
            .findRecentPublished(EpisodeStatus.PUBLISHED, VisibilityStatus.PUBLISHED,
                PageRequest.of(0, RECENT_SECTION_LIMIT))
            .stream()
            .map(this::toRecentEpisodeItem)
            .toList();

        if (section.getMode() == HomeSectionMode.AUTOMATIC) {
            return automaticItems;
        }

        LinkedHashMap<String, HomeSectionItemResponse> merged = Stream.concat(manualItems.stream(), automaticItems.stream())
            .filter(item -> item.episodeId() != null)
            .collect(
                LinkedHashMap::new,
                (map, item) -> map.putIfAbsent(item.episodeId(), item),
                LinkedHashMap::putAll
            );

        return merged.values().stream()
            .limit(RECENT_SECTION_LIMIT)
            .toList();
    }

    private HomeSectionItemResponse toHeroItem(HeroConfigEntity hero) {
        AnimeEntity anime = hero.getAnime();
        if (anime == null) {
            return null;
        }
        return new HomeSectionItemResponse(
            hero.getId().toString(),
            anime.getId().toString(),
            null,
            anime.getTitleDisplay(),
            hero.getHeadline(),
            anime.getCoverUrl(),
            anime.getBannerUrl(),
            null,
            anime.getSlug(),
            anime.getSynopsis(),
            anime.getType().name(),
            anime.getStatus().name(),
            anime.getSeasonLabel(),
            anime.getYear(),
            anime.getStudio(),
            anime.getAverageScore() != null ? anime.getAverageScore().doubleValue() : null,
            displayGenres(anime)
        );
    }

    private HomeSectionItemResponse toSectionItem(HomeSectionItemEntity item) {
        if (item.getAnime() != null) {
            AnimeEntity anime = item.getAnime();
            return new HomeSectionItemResponse(
                item.getId().toString(),
                anime.getId().toString(),
                null,
                anime.getTitleDisplay(),
                anime.getSeasonLabel(),
                anime.getCoverUrl(),
                anime.getBannerUrl(),
                null,
                anime.getSlug(),
                anime.getSynopsis(),
                anime.getType().name(),
                anime.getStatus().name(),
                anime.getSeasonLabel(),
                anime.getYear(),
                anime.getStudio(),
                anime.getAverageScore() != null ? anime.getAverageScore().doubleValue() : null,
                displayGenres(anime)
            );
        }

        EpisodeEntity episode = item.getEpisode();
        return toRecentEpisodeItem(episode, item.getId().toString());
    }

    private HomeSectionItemResponse toRecentEpisodeItem(EpisodeEntity episode) {
        return toRecentEpisodeItem(episode, episode.getId().toString());
    }

    private HomeSectionItemResponse toRecentEpisodeItem(EpisodeEntity episode, String itemId) {
        return new HomeSectionItemResponse(
            itemId,
            episode.getAnime().getId().toString(),
            episode.getId().toString(),
            episode.getAnime().getTitleDisplay(),
            "Ep. " + episode.getNumber(),
            episode.getAnime().getCoverUrl(),
            episode.getThumbnailUrl(),
            episode.getPreviewUrl(),
            episode.getAnime().getSlug(),
            episode.getAnime().getSynopsis(),
            episode.getAnime().getType().name(),
            episode.getAnime().getStatus().name(),
            episode.getAnime().getSeasonLabel(),
            episode.getAnime().getYear(),
            episode.getAnime().getStudio(),
            episode.getAnime().getAverageScore() != null ? episode.getAnime().getAverageScore().doubleValue() : null,
            displayGenres(episode.getAnime())
        );
    }

    private List<String> displayGenres(AnimeEntity anime) {
        return displayGenres(anime.getGenres(), anime.getType(), anime.getStatus());
    }

    private List<String> displayGenres(List<String> genres, AnimeType type, AnimeStatus status) {
        if (genres != null && !genres.isEmpty()) {
            return genres;
        }
        return Stream.of(type, status)
            .filter(java.util.Objects::nonNull)
            .map(Enum::name)
            .map(value -> value.replace('_', ' '))
            .limit(3)
            .toList();
    }

    private List<String> parseGenres(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\n"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

}
