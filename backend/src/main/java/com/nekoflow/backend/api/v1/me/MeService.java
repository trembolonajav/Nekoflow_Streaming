package com.nekoflow.backend.api.v1.me;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;
import com.nekoflow.backend.api.v1.me.dto.ContinueWatchingItemResponse;
import com.nekoflow.backend.api.v1.me.dto.HistoryItemResponse;
import com.nekoflow.backend.api.v1.me.dto.ProfileResponse;
import com.nekoflow.backend.api.v1.me.dto.ProfileStatsResponse;
import com.nekoflow.backend.api.v1.me.dto.UpdatePreferencesRequest;
import com.nekoflow.backend.api.v1.me.dto.UpsertProgressRequest;
import com.nekoflow.backend.api.v1.me.dto.UserPreferencesResponse;
import com.nekoflow.backend.api.v1.me.dto.WatchlistItemResponse;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.entity.UserPreferenceEntity;
import com.nekoflow.backend.domain.entity.WatchHistoryEntity;
import com.nekoflow.backend.domain.entity.WatchProgressEntity;
import com.nekoflow.backend.domain.entity.WatchlistEntity;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.CommentRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.RefreshTokenRepository;
import com.nekoflow.backend.domain.repository.UserPreferenceRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.domain.repository.WatchHistoryRepository;
import com.nekoflow.backend.domain.repository.WatchProgressRepository;
import com.nekoflow.backend.domain.repository.WatchlistRepository;
import com.nekoflow.backend.security.AppUserPrincipal;

@Service
public class MeService {

    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final WatchProgressRepository watchProgressRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public MeService(
        UserRepository userRepository,
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        WatchProgressRepository watchProgressRepository,
        WatchHistoryRepository watchHistoryRepository,
        WatchlistRepository watchlistRepository,
        UserPreferenceRepository userPreferenceRepository,
        CommentRepository commentRepository,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.watchProgressRepository = watchProgressRepository;
        this.watchHistoryRepository = watchHistoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.commentRepository = commentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        UserEntity user = currentUser();
        List<ContinueWatchingItemResponse> continueWatching = watchProgressRepository.findTop12ByUserIdOrderByLastWatchedAtDesc(user.getId()).stream()
            .map(this::toContinueItem)
            .toList();

        return new ProfileResponse(
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            user.getAvatarUrl(),
            new ProfileStatsResponse(
                continueWatching.size(),
                Math.toIntExact(watchlistRepository.countByUserId(user.getId())),
                Math.toIntExact(watchHistoryRepository.countByUserId(user.getId())),
                Math.toIntExact(commentRepository.countByUserIdAndStatus(user.getId(), "VISIBLE"))
            ),
            continueWatching
        );
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> getWatchlist() {
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser().getId()).stream()
            .map(this::toWatchlistItem)
            .toList();
    }

    @Transactional
    public WatchlistItemResponse addToWatchlist(UUID animeId) {
        UserEntity user = currentUser();
        WatchlistEntity existing = watchlistRepository.findByUserIdAndAnimeId(user.getId(), animeId).orElse(null);
        if (existing != null) {
            return toWatchlistItem(existing);
        }

        AnimeEntity anime = animeRepository.findById(animeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime not found"));

        WatchlistEntity entry = new WatchlistEntity();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        entry.setAnime(anime);
        entry.setStatus("WATCHING");
        return toWatchlistItem(watchlistRepository.save(entry));
    }

    @Transactional
    public ApiMessageResponse removeFromWatchlist(UUID animeId) {
        UserEntity user = currentUser();
        watchlistRepository.findByUserIdAndAnimeId(user.getId(), animeId)
            .ifPresent(watchlistRepository::delete);
        return new ApiMessageResponse("Anime removido da lista.");
    }

    @Transactional(readOnly = true)
    public List<HistoryItemResponse> getHistory() {
        return watchHistoryRepository.findTop20ByUserIdOrderByWatchedAtDesc(currentUser().getId()).stream()
            .map(this::toHistoryItem)
            .toList();
    }

    @Transactional
    public ApiMessageResponse deleteHistoryItem(UUID historyId) {
        UserEntity user = currentUser();
        WatchHistoryEntity item = watchHistoryRepository.findById(historyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "History item does not belong to user");
        }
        watchHistoryRepository.delete(item);
        return new ApiMessageResponse("Item removido do histórico.");
    }

    @Transactional
    public ApiMessageResponse clearHistory() {
        watchHistoryRepository.deleteByUserId(currentUser().getId());
        return new ApiMessageResponse("Histórico limpo.");
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences() {
        return toPreferencesResponse(getOrCreatePreferences(currentUser()));
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
        UserPreferenceEntity preferences = getOrCreatePreferences(currentUser());
        preferences.setAutoplay(request.autoplay());
        preferences.setAutoNext(request.autoNext());
        preferences.setPreferredAudio(request.preferredAudio());
        preferences.setPreferredSubtitle(request.preferredSubtitle());
        preferences.setPreferredQuality(request.preferredQuality());
        preferences.setNotifyReleases(request.notifyReleases());
        preferences.setNotifyNewEpisodes(request.notifyNewEpisodes());
        preferences.setNotifyWatchlist(request.notifyWatchlist());
        preferences.setNotifyMarketing(request.notifyMarketing());
        return toPreferencesResponse(userPreferenceRepository.save(preferences));
    }

    @Transactional
    public ApiMessageResponse upsertProgress(UpsertProgressRequest request) {
        UserEntity user = currentUser();
        AnimeEntity anime = animeRepository.findById(request.animeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime not found"));
        EpisodeEntity episode = episodeRepository.findById(request.episodeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found"));

        if (!episode.getAnime().getId().equals(anime.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Episode does not belong to anime");
        }

        int clampedSeconds = Math.max(0, Math.min(request.progressSeconds(), request.durationSeconds()));
        BigDecimal progressPercent = BigDecimal.valueOf(clampedSeconds)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(request.durationSeconds()), 2, RoundingMode.HALF_UP);

        WatchProgressEntity progress = watchProgressRepository.findByUserIdAndAnimeId(user.getId(), anime.getId())
            .orElseGet(() -> {
                WatchProgressEntity entity = new WatchProgressEntity();
                entity.setId(UUID.randomUUID());
                entity.setUser(user);
                entity.setAnime(anime);
                return entity;
            });

        progress.setEpisode(episode);
        progress.setProgressSeconds(clampedSeconds);
        progress.setProgressPercent(progressPercent);
        progress.setLastWatchedAt(OffsetDateTime.now());
        watchProgressRepository.save(progress);

        OffsetDateTime now = OffsetDateTime.now();
        watchHistoryRepository.findTop1ByUserIdAndEpisodeIdOrderByWatchedAtDesc(user.getId(), episode.getId())
            .filter(history -> history.getWatchedAt().isAfter(now.minusMinutes(30)))
            .orElseGet(() -> {
                WatchHistoryEntity history = new WatchHistoryEntity();
                history.setId(UUID.randomUUID());
                history.setUser(user);
                history.setAnime(anime);
                history.setEpisode(episode);
                history.setWatchedAt(now);
                return watchHistoryRepository.save(history);
            });

        return new ApiMessageResponse("Progresso atualizado.");
    }

    @Transactional(readOnly = true)
    public List<com.nekoflow.backend.domain.entity.RefreshTokenEntity> getActiveSessions() {
        return refreshTokenRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUser().getId());
    }

    @Transactional
    public ApiMessageResponse revokeSession(UUID sessionId) {
        UserEntity user = currentUser();
        com.nekoflow.backend.domain.entity.RefreshTokenEntity session = refreshTokenRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to user");
        }
        session.setRevoked(true);
        refreshTokenRepository.save(session);
        return new ApiMessageResponse("Sessão encerrada.");
    }

    private UserEntity currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findById(appUserPrincipal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ContinueWatchingItemResponse toContinueItem(WatchProgressEntity progress) {
        Integer duration = progress.getEpisode().getDurationSeconds();
        int remainingMinutes = duration != null
            ? Math.max(1, (int) Math.ceil((duration - progress.getProgressSeconds()) / 60.0))
            : 0;

        return new ContinueWatchingItemResponse(
            progress.getAnime().getId().toString(),
            progress.getAnime().getSlug(),
            progress.getAnime().getTitleDisplay(),
            progress.getEpisode().getId().toString(),
            progress.getEpisode().getNumber(),
            progress.getEpisode().getTitle(),
            progress.getEpisode().getThumbnailUrl(),
            progress.getProgressSeconds(),
            progress.getProgressPercent() != null ? progress.getProgressPercent().doubleValue() : 0,
            remainingMinutes
        );
    }

    private WatchlistItemResponse toWatchlistItem(WatchlistEntity entry) {
        return new WatchlistItemResponse(
            entry.getId().toString(),
            entry.getAnime().getId().toString(),
            entry.getAnime().getSlug(),
            entry.getAnime().getTitleDisplay(),
            entry.getAnime().getCoverUrl(),
            entry.getStatus(),
            entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : null
        );
    }

    private HistoryItemResponse toHistoryItem(WatchHistoryEntity item) {
        return new HistoryItemResponse(
            item.getId().toString(),
            item.getAnime().getId().toString(),
            item.getAnime().getSlug(),
            item.getAnime().getTitleDisplay(),
            item.getEpisode().getId().toString(),
            item.getEpisode().getNumber(),
            item.getEpisode().getTitle(),
            item.getEpisode().getThumbnailUrl(),
            item.getWatchedAt().toString()
        );
    }

    private UserPreferenceEntity getOrCreatePreferences(UserEntity user) {
        return userPreferenceRepository.findById(user.getId())
            .orElseGet(() -> {
                UserPreferenceEntity preferences = new UserPreferenceEntity();
                preferences.setUser(user);
                preferences.setUserId(user.getId());
                preferences.setAutoplay(true);
                preferences.setAutoNext(true);
                preferences.setPreferredAudio("ja");
                preferences.setPreferredSubtitle("pt-BR");
                preferences.setPreferredQuality("auto");
                preferences.setNotifyReleases(true);
                preferences.setNotifyNewEpisodes(true);
                preferences.setNotifyWatchlist(true);
                preferences.setNotifyMarketing(false);
                return userPreferenceRepository.save(preferences);
            });
    }

    private UserPreferencesResponse toPreferencesResponse(UserPreferenceEntity preferences) {
        return new UserPreferencesResponse(
            preferences.isAutoplay(),
            preferences.isAutoNext(),
            preferences.getPreferredAudio(),
            preferences.getPreferredSubtitle(),
            preferences.getPreferredQuality(),
            preferences.isNotifyReleases(),
            preferences.isNotifyNewEpisodes(),
            preferences.isNotifyWatchlist(),
            preferences.isNotifyMarketing()
        );
    }
}
