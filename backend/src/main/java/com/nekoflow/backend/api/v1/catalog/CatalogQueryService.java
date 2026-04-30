package com.nekoflow.backend.api.v1.catalog;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeEpisodeSummaryResponse;
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

    public List<AnimeSummaryResponse> listPublishedAnimes() {
        return animeRepository.findAll().stream()
            .filter(anime -> anime.getVisibility() == VisibilityStatus.PUBLISHED)
            .sorted(Comparator.comparing(AnimeEntity::getTitleDisplay))
            .map(this::toSummary)
            .toList();
    }

    public Optional<AnimeDetailResponse> findPublishedAnimeBySlug(String slug) {
        return animeRepository.findBySlugAndVisibility(slug, VisibilityStatus.PUBLISHED)
            .map(this::toDetail);
    }

    public Optional<WatchPlayerResponse> findPublishedEpisodePlayer(String slug, Integer episodeNumber) {
        return episodeRepository.findByAnimeSlugAndNumberAndStatus(slug, episodeNumber, EpisodeStatus.PUBLISHED)
            .map(this::toWatchPlayer);
    }

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

    private AnimeSummaryResponse toSummary(AnimeEntity anime) {
        return new AnimeSummaryResponse(
            anime.getId().toString(),
            anime.getSlug(),
            anime.getAnilistId(),
            anime.getTitleDisplay(),
            anime.getTitleRomaji(),
            anime.getCoverUrl(),
            anime.getBannerUrl(),
            anime.getType().name(),
            anime.getStatus().name(),
            anime.getVisibility().name(),
            anime.getYear()
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
            List.of(),
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

        List<HomeSectionItemResponse> automaticItems = episodeRepository.findAllByOrderByPublishedAtDescNumberDesc().stream()
            .filter(episode -> episode.getStatus() == EpisodeStatus.PUBLISHED)
            .filter(episode -> episode.getAnime() != null && episode.getAnime().getVisibility() == VisibilityStatus.PUBLISHED)
            .map(this::toRecentEpisodeItem)
            .limit(RECENT_SECTION_LIMIT)
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
            anime.getSlug()
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
                anime.getSlug()
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
            episode.getAnime().getSlug()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
