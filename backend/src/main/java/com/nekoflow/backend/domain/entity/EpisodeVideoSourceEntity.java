package com.nekoflow.backend.domain.entity;

import java.util.UUID;

import com.nekoflow.backend.domain.enums.VideoProvider;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "episode_video_sources")
public class EpisodeVideoSourceEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private EpisodeEntity episode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoProvider provider;

    @Column(name = "external_video_id")
    private String externalVideoId;

    @Column(name = "embed_url")
    private String embedUrl;

    @Column(name = "player_url")
    private String playerUrl;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoSourceStatus status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EpisodeEntity getEpisode() {
        return episode;
    }

    public void setEpisode(EpisodeEntity episode) {
        this.episode = episode;
    }

    public VideoProvider getProvider() {
        return provider;
    }

    public void setProvider(VideoProvider provider) {
        this.provider = provider;
    }

    public String getExternalVideoId() {
        return externalVideoId;
    }

    public void setExternalVideoId(String externalVideoId) {
        this.externalVideoId = externalVideoId;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public String getPlayerUrl() {
        return playerUrl;
    }

    public void setPlayerUrl(String playerUrl) {
        this.playerUrl = playerUrl;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public VideoSourceStatus getStatus() {
        return status;
    }

    public void setStatus(VideoSourceStatus status) {
        this.status = status;
    }
}
