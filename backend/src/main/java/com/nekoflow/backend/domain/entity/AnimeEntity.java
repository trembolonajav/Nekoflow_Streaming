package com.nekoflow.backend.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.VisibilityStatus;

import com.nekoflow.backend.domain.SearchText;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "anime")
public class AnimeEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "anilist_id")
    private Long anilistId;

    @Column(name = "title_display", nullable = false)
    private String titleDisplay;

    @Column(name = "title_romaji")
    private String titleRomaji;

    @Column(name = "title_native")
    private String titleNative;

    @Column(name = "title_english")
    private String titleEnglish;

    private String synopsis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimeStatus status;

    @Column(name = "season_label")
    private String seasonLabel;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisibilityStatus visibility;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "average_score")
    private BigDecimal averageScore;

    private String studio;

    @Column(columnDefinition = "text")
    private String genres;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    // Curadoria do calendario: quando false, o anime some do calendario publico
    // e a sincronizacao com o AniList deixa de criar/atualizar episodios agendados.
    @Column(name = "show_in_calendar", nullable = false)
    private boolean showInCalendar = true;

    // Texto normalizado (minusculo/sem acento) para busca no banco. Mantido pelo
    // callback abaixo em todos os caminhos de escrita (worker, admin, conversao).
    @Column(name = "search_index", columnDefinition = "text")
    private String searchIndex;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL)
    private List<EpisodeEntity> episodes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void refreshSearchIndex() {
        this.searchIndex = SearchText.normalize(String.join(" ",
            nullToBlank(titleDisplay),
            nullToBlank(titleRomaji),
            nullToBlank(titleEnglish),
            nullToBlank(titleNative),
            nullToBlank(slug),
            nullToBlank(studio),
            nullToBlank(seasonLabel),
            nullToBlank(genres),
            year == null ? "" : year.toString()
        ));
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Long getAnilistId() {
        return anilistId;
    }

    public void setAnilistId(Long anilistId) {
        this.anilistId = anilistId;
    }

    public String getTitleDisplay() {
        return titleDisplay;
    }

    public void setTitleDisplay(String titleDisplay) {
        this.titleDisplay = titleDisplay;
    }

    public String getTitleRomaji() {
        return titleRomaji;
    }

    public void setTitleRomaji(String titleRomaji) {
        this.titleRomaji = titleRomaji;
    }

    public String getTitleNative() {
        return titleNative;
    }

    public void setTitleNative(String titleNative) {
        this.titleNative = titleNative;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public void setTitleEnglish(String titleEnglish) {
        this.titleEnglish = titleEnglish;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public AnimeType getType() {
        return type;
    }

    public void setType(AnimeType type) {
        this.type = type;
    }

    public AnimeStatus getStatus() {
        return status;
    }

    public void setStatus(AnimeStatus status) {
        this.status = status;
    }

    public String getSeasonLabel() {
        return seasonLabel;
    }

    public void setSeasonLabel(String seasonLabel) {
        this.seasonLabel = seasonLabel;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public VisibilityStatus getVisibility() {
        return visibility;
    }

    public void setVisibility(VisibilityStatus visibility) {
        this.visibility = visibility;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public String getStudio() {
        return studio;
    }

    public void setStudio(String studio) {
        this.studio = studio;
    }

    public List<String> getGenres() {
        if (genres == null || genres.isBlank()) {
            return List.of();
        }
        return Arrays.stream(genres.split("\\n"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    public void setGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            this.genres = null;
            return;
        }
        this.genres = String.join(
            "\n",
            genres.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList()
        );
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isShowInCalendar() {
        return showInCalendar;
    }

    public void setShowInCalendar(boolean showInCalendar) {
        this.showInCalendar = showInCalendar;
    }

    public List<EpisodeEntity> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<EpisodeEntity> episodes) {
        this.episodes = episodes;
    }
}
