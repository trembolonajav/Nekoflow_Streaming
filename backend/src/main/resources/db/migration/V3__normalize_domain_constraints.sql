ALTER TABLE roles
    ADD CONSTRAINT chk_roles_code
    CHECK (code IN ('ADMIN', 'EDITOR', 'MODERATOR', 'USER'));

ALTER TABLE anime
    ADD CONSTRAINT chk_anime_type
    CHECK (type IN ('SERIES', 'MOVIE', 'OVA', 'SPECIAL'));

ALTER TABLE anime
    ADD CONSTRAINT chk_anime_status
    CHECK (status IN ('RELEASING', 'FINISHED', 'HIATUS'));

ALTER TABLE anime
    ADD CONSTRAINT chk_anime_visibility
    CHECK (visibility IN ('DRAFT', 'REVIEW', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'));

ALTER TABLE episodes
    ADD CONSTRAINT chk_episodes_status
    CHECK (status IN ('DRAFT', 'REVIEW', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'));

ALTER TABLE episode_video_sources
    ADD CONSTRAINT chk_episode_video_sources_provider
    CHECK (provider IN ('SEEKSTREAMING'));

ALTER TABLE episode_video_sources
    ADD CONSTRAINT chk_episode_video_sources_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE home_sections
    ADD CONSTRAINT chk_home_sections_mode
    CHECK (mode IN ('MANUAL', 'AUTOMATIC', 'HYBRID'));
