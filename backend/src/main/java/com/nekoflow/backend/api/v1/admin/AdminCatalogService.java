package com.nekoflow.backend.api.v1.admin;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.admin.dto.AdminAnimeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminAnimeUpsertRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminEpisodeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminEpisodeUpsertRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminHeroRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminHeroResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminHomeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminHomeSectionRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminHomeSectionResponse;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.EpisodeVideoSourceEntity;
import com.nekoflow.backend.domain.entity.HeroConfigEntity;
import com.nekoflow.backend.domain.entity.HomeSectionEntity;
import com.nekoflow.backend.domain.entity.HomeSectionItemEntity;
import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.HomeSectionMode;
import com.nekoflow.backend.domain.enums.VideoProvider;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.EpisodeVideoSourceRepository;
import com.nekoflow.backend.domain.repository.HeroConfigRepository;
import com.nekoflow.backend.domain.repository.HomeSectionItemRepository;
import com.nekoflow.backend.domain.repository.HomeSectionRepository;

@Service
public class AdminCatalogService {

    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeVideoSourceRepository episodeVideoSourceRepository;
    private final HomeSectionRepository homeSectionRepository;
    private final HomeSectionItemRepository homeSectionItemRepository;
    private final HeroConfigRepository heroConfigRepository;
    private final com.nekoflow.backend.api.v1.catalog.CatalogCache catalogCache;

    public AdminCatalogService(
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        EpisodeVideoSourceRepository episodeVideoSourceRepository,
        HomeSectionRepository homeSectionRepository,
        HomeSectionItemRepository homeSectionItemRepository,
        HeroConfigRepository heroConfigRepository,
        com.nekoflow.backend.api.v1.catalog.CatalogCache catalogCache
    ) {
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.episodeVideoSourceRepository = episodeVideoSourceRepository;
        this.homeSectionRepository = homeSectionRepository;
        this.homeSectionItemRepository = homeSectionItemRepository;
        this.heroConfigRepository = heroConfigRepository;
        this.catalogCache = catalogCache;
    }

    @Transactional(readOnly = true)
    public List<AdminAnimeResponse> listAnimes() {
        return animeRepository.findAllByOrderByTitleDisplayAsc().stream()
            .map(this::toAdminAnime)
            .toList();
    }

    @Transactional
    public AdminAnimeResponse createAnime(AdminAnimeUpsertRequest request) {
        AnimeEntity anime = new AnimeEntity();
        anime.setId(UUID.randomUUID());
        applyAnimeRequest(anime, request);
        AdminAnimeResponse response = toAdminAnime(animeRepository.save(anime));
        catalogCache.invalidateAll();
        return response;
    }

    @Transactional
    public AdminAnimeResponse updateAnime(UUID id, AdminAnimeUpsertRequest request) {
        AnimeEntity anime = requireAnime(id);
        applyAnimeRequest(anime, request);
        AdminAnimeResponse response = toAdminAnime(animeRepository.save(anime));
        catalogCache.invalidateAll();
        return response;
    }

    @Transactional
    public AdminAnimeResponse updateAnimeVisibility(UUID id, String visibility) {
        AnimeEntity anime = requireAnime(id);
        VisibilityStatus status = parseEnum(VisibilityStatus.class, visibility, "visibility");
        anime.setVisibility(status);
        anime.setPublishedAt(status == VisibilityStatus.PUBLISHED ? OffsetDateTime.now() : null);
        AdminAnimeResponse response = toAdminAnime(animeRepository.save(anime));
        catalogCache.invalidateAll();
        return response;
    }

    @Transactional
    public void deleteAnime(UUID id) {
        AnimeEntity anime = requireAnime(id);
        animeRepository.delete(anime);
        catalogCache.invalidateAll();
    }

    @Transactional(readOnly = true)
    public List<AdminEpisodeResponse> listEpisodes() {
        return episodeRepository.findAllByOrderByPublishedAtDescNumberDesc().stream()
            .map(this::toAdminEpisode)
            .toList();
    }

    @Transactional
    public AdminEpisodeResponse createEpisode(AdminEpisodeUpsertRequest request) {
        AnimeEntity anime = requireAnime(parseUuid(request.animeId(), "animeId"));
        validateEpisodeNumberUniqueness(anime, request.number(), null);
        EpisodeEntity episode = new EpisodeEntity();
        episode.setId(UUID.randomUUID());
        episode.setAnime(anime);
        applyEpisodeRequest(episode, request);
        EpisodeEntity saved = episodeRepository.save(episode);
        replaceVideoSource(saved, request);
        AdminEpisodeResponse response = toAdminEpisode(saved);
        catalogCache.invalidateAll();
        return response;
    }

    @Transactional
    public AdminEpisodeResponse updateEpisode(UUID id, AdminEpisodeUpsertRequest request) {
        EpisodeEntity episode = requireEpisode(id);
        AnimeEntity anime = requireAnime(parseUuid(request.animeId(), "animeId"));
        validateEpisodeNumberUniqueness(anime, request.number(), id);
        episode.setAnime(anime);
        applyEpisodeRequest(episode, request);
        EpisodeEntity saved = episodeRepository.save(episode);
        replaceVideoSource(saved, request);
        AdminEpisodeResponse response = toAdminEpisode(saved);
        catalogCache.invalidateAll();
        return response;
    }

    @Transactional
    public void deleteEpisode(UUID id) {
        EpisodeEntity episode = requireEpisode(id);
        episodeRepository.delete(episode);
        catalogCache.invalidateAll();
    }

    @Transactional(readOnly = true)
    public AdminHomeResponse getHomeConfig() {
        List<AdminHomeSectionResponse> sections = homeSectionRepository.findAllByOrderBySortOrderAsc().stream()
            .map(this::toAdminHomeSection)
            .toList();
        List<HeroConfigEntity> heroItems = heroConfigRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc();
        String tag = heroItems.isEmpty() ? "Destaque editorial" : heroItems.get(0).getHeadline();
        String ctaLabel = heroItems.isEmpty() ? "Assistir agora" : heroItems.get(0).getCtaLabel();

        return new AdminHomeResponse(
            new AdminHeroResponse(
                heroItems.stream()
                    .map(HeroConfigEntity::getAnime)
                    .filter(java.util.Objects::nonNull)
                    .map(anime -> anime.getId().toString())
                    .toList(),
                heroItems.isEmpty() ? "Destaque editorial" : heroItems.get(0).getHeadline(),
                ctaLabel
            ),
            sections
        );
    }

    @Transactional
    public AdminHomeResponse updateSections(List<AdminHomeSectionRequest> requests) {
        for (AdminHomeSectionRequest request : requests) {
            HomeSectionEntity section = homeSectionRepository.findByCode(request.code())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found: " + request.code()));
            section.setMode(parseEnum(HomeSectionMode.class, request.mode(), "mode"));
            section.setActive(request.active());
            section.setSortOrder(request.sortOrder());
            homeSectionRepository.save(section);

            homeSectionItemRepository.deleteBySection(section);
            for (int index = 0; index < request.manualItemIds().size(); index++) {
                String itemId = request.manualItemIds().get(index);
                HomeSectionItemEntity item = new HomeSectionItemEntity();
                item.setId(UUID.randomUUID());
                item.setSection(section);
                item.setSortOrder(index + 1);
                if ("recent".equals(section.getCode())) {
                    item.setEpisode(requireEpisode(parseUuid(itemId, "manualItemId")));
                } else {
                    item.setAnime(requireAnime(parseUuid(itemId, "manualItemId")));
                }
                homeSectionItemRepository.save(item);
            }
        }
        return getHomeConfig();
    }

    @Transactional
    public AdminHeroResponse updateHero(AdminHeroRequest request) {
        heroConfigRepository.deleteAll();
        for (int index = 0; index < request.animeIds().size(); index++) {
            HeroConfigEntity hero = new HeroConfigEntity();
            hero.setId(UUID.randomUUID());
            hero.setAnime(requireAnime(parseUuid(request.animeIds().get(index), "animeId")));
            hero.setHeadline(blankToNull(request.tag()));
            hero.setCtaLabel(blankToNull(request.ctaLabel()));
            hero.setSubheadline(null);
            hero.setActive(true);
            hero.setSortOrder(index + 1);
            heroConfigRepository.save(hero);
        }
        return getHomeConfig().hero();
    }

    private void applyAnimeRequest(AnimeEntity anime, AdminAnimeUpsertRequest request) {
        anime.setAnilistId(request.anilistId());
        anime.setSlug(request.slug().trim());
        anime.setTitleDisplay(request.titleDisplay().trim());
        anime.setTitleRomaji(blankToNull(request.titleRomaji()));
        anime.setTitleNative(blankToNull(request.titleNative()));
        anime.setTitleEnglish(blankToNull(request.titleEnglish()));
        anime.setSynopsis(blankToNull(request.synopsis()));
        anime.setType(parseEnum(AnimeType.class, request.type(), "type"));
        anime.setStatus(parseEnum(AnimeStatus.class, request.status(), "status"));
        VisibilityStatus visibility = parseEnum(VisibilityStatus.class, request.visibility(), "visibility");
        anime.setVisibility(visibility);
        anime.setSeasonLabel(blankToNull(request.seasonLabel()));
        anime.setYear(request.year());
        anime.setCoverUrl(blankToNull(request.coverUrl()));
        anime.setBannerUrl(blankToNull(request.bannerUrl()));
        anime.setStudio(blankToNull(request.studio()));
        anime.setGenres(request.genres());
        anime.setPublishedAt(visibility == VisibilityStatus.PUBLISHED ? OffsetDateTime.now() : null);
    }

    private void applyEpisodeRequest(EpisodeEntity episode, AdminEpisodeUpsertRequest request) {
        episode.setNumber(request.number());
        episode.setTitle(request.title().trim());
        episode.setSummary(blankToNull(request.summary()));
        episode.setDurationSeconds(request.durationSeconds());
        episode.setThumbnailUrl(blankToNull(request.thumbnailUrl()));
        episode.setPreviewUrl(blankToNull(request.previewUrl()));
        EpisodeStatus status = parseEnum(EpisodeStatus.class, request.status(), "status");
        episode.setStatus(status);
        episode.setScheduledFor(parseDateTime(request.scheduledFor()));
        episode.setPublishedAt(status == EpisodeStatus.PUBLISHED ? OffsetDateTime.now() : null);
    }

    private void replaceVideoSource(EpisodeEntity episode, AdminEpisodeUpsertRequest request) {
        episodeVideoSourceRepository.deleteByEpisode(episode);
        episode.getVideoSources().clear();
        if (isBlank(request.embedUrl()) && isBlank(request.playerUrl()) && isBlank(request.externalVideoId())) {
            return;
        }

        EpisodeVideoSourceEntity source = new EpisodeVideoSourceEntity();
        source.setId(UUID.randomUUID());
        source.setEpisode(episode);
        source.setProvider(parseEnum(VideoProvider.class, defaultIfBlank(request.provider(), "SEEKSTREAMING"), "provider"));
        source.setExternalVideoId(blankToNull(request.externalVideoId()));
        source.setEmbedUrl(EmbedUrlValidator.validateOrThrow(request.embedUrl()));
        source.setPlayerUrl(EmbedUrlValidator.validateOrThrow(request.playerUrl()));
        source.setDefault(true);
        source.setStatus(VideoSourceStatus.ACTIVE);
        episode.getVideoSources().add(source);
    }

    private void validateEpisodeNumberUniqueness(AnimeEntity anime, Integer number, UUID currentEpisodeId) {
        boolean exists = currentEpisodeId == null
            ? episodeRepository.existsByAnimeIdAndNumber(anime.getId(), number)
            : episodeRepository.existsByAnimeIdAndNumberAndIdNot(anime.getId(), number, currentEpisodeId);

        if (exists) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Já existe um episódio " + number + " para " + anime.getTitleDisplay() + "."
            );
        }
    }

    private AdminAnimeResponse toAdminAnime(AnimeEntity anime) {
        return new AdminAnimeResponse(
            anime.getId().toString(),
            anime.getAnilistId(),
            anime.getSlug(),
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
            anime.getGenres(),
            anime.getEpisodes() != null ? anime.getEpisodes().size() : 0,
            anime.getPublishedAt() != null ? anime.getPublishedAt().toString() : null,
            anime.isShowInCalendar()
        );
    }

    private AdminEpisodeResponse toAdminEpisode(EpisodeEntity episode) {
        EpisodeVideoSourceEntity source = episode.getVideoSources().stream().findFirst().orElse(null);
        return new AdminEpisodeResponse(
            episode.getId().toString(),
            episode.getAnime().getId().toString(),
            episode.getAnime().getTitleDisplay(),
            episode.getNumber(),
            episode.getTitle(),
            episode.getSummary(),
            episode.getDurationSeconds(),
            episode.getThumbnailUrl(),
            episode.getPreviewUrl(),
            episode.getStatus().name(),
            episode.getScheduledFor() != null ? episode.getScheduledFor().toString() : null,
            source != null ? source.getProvider().name() : null,
            source != null ? source.getExternalVideoId() : null,
            source != null ? source.getEmbedUrl() : null,
            source != null ? source.getPlayerUrl() : null,
            episode.getPublishedAt() != null ? episode.getPublishedAt().toString() : null
        );
    }

    private AdminHomeSectionResponse toAdminHomeSection(HomeSectionEntity section) {
        List<String> manualItemIds = section.getItems().stream()
            .map(item -> item.getEpisode() != null ? item.getEpisode().getId().toString() : item.getAnime().getId().toString())
            .toList();
        return new AdminHomeSectionResponse(
            section.getCode(),
            section.getTitle(),
            section.getMode().name(),
            section.isActive(),
            section.getSortOrder(),
            manualItemIds
        );
    }

    private AnimeEntity requireAnime(UUID id) {
        return animeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime not found"));
    }

    private EpisodeEntity requireEpisode(UUID id) {
        return episodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found"));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName, exception);
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid datetime", exception);
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing " + fieldName);
        }
        try {
            return Enum.valueOf(enumClass, normalizeEnumValue(value));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName + ": " + value, exception);
        }
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
