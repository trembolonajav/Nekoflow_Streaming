package com.nekoflow.backend.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferenceEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private boolean autoplay;

    @Column(name = "auto_next", nullable = false)
    private boolean autoNext;

    @Column(name = "preferred_audio", nullable = false)
    private String preferredAudio;

    @Column(name = "preferred_subtitle", nullable = false)
    private String preferredSubtitle;

    @Column(name = "preferred_quality", nullable = false)
    private String preferredQuality;

    @Column(name = "notify_releases", nullable = false)
    private boolean notifyReleases;

    @Column(name = "notify_new_episodes", nullable = false)
    private boolean notifyNewEpisodes;

    @Column(name = "notify_watchlist", nullable = false)
    private boolean notifyWatchlist;

    @Column(name = "notify_marketing", nullable = false)
    private boolean notifyMarketing;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public void setAutoplay(boolean autoplay) {
        this.autoplay = autoplay;
    }

    public boolean isAutoNext() {
        return autoNext;
    }

    public void setAutoNext(boolean autoNext) {
        this.autoNext = autoNext;
    }

    public String getPreferredAudio() {
        return preferredAudio;
    }

    public void setPreferredAudio(String preferredAudio) {
        this.preferredAudio = preferredAudio;
    }

    public String getPreferredSubtitle() {
        return preferredSubtitle;
    }

    public void setPreferredSubtitle(String preferredSubtitle) {
        this.preferredSubtitle = preferredSubtitle;
    }

    public String getPreferredQuality() {
        return preferredQuality;
    }

    public void setPreferredQuality(String preferredQuality) {
        this.preferredQuality = preferredQuality;
    }

    public boolean isNotifyReleases() {
        return notifyReleases;
    }

    public void setNotifyReleases(boolean notifyReleases) {
        this.notifyReleases = notifyReleases;
    }

    public boolean isNotifyNewEpisodes() {
        return notifyNewEpisodes;
    }

    public void setNotifyNewEpisodes(boolean notifyNewEpisodes) {
        this.notifyNewEpisodes = notifyNewEpisodes;
    }

    public boolean isNotifyWatchlist() {
        return notifyWatchlist;
    }

    public void setNotifyWatchlist(boolean notifyWatchlist) {
        this.notifyWatchlist = notifyWatchlist;
    }

    public boolean isNotifyMarketing() {
        return notifyMarketing;
    }

    public void setNotifyMarketing(boolean notifyMarketing) {
        this.notifyMarketing = notifyMarketing;
    }
}
