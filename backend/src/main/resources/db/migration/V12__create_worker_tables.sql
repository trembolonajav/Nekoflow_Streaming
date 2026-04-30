CREATE TABLE seek_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seek_video_id TEXT NOT NULL UNIQUE,
    filename TEXT,
    title TEXT,
    thumbnail_url TEXT,
    duration_seconds INTEGER,
    size_bytes BIGINT,
    seek_status TEXT,
    raw_payload JSONB,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seek_videos_status ON seek_videos (seek_status);
CREATE INDEX idx_seek_videos_synced ON seek_videos (last_synced_at DESC);

CREATE TABLE release_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seek_video_id TEXT UNIQUE,
    source TEXT NOT NULL,
    parsed_title TEXT,
    parsed_season INTEGER,
    parsed_episode INTEGER,
    anilist_id INTEGER,
    anilist_payload JSONB,
    match_confidence NUMERIC(4,3),
    matched_anime_id TEXT,
    thumbnail_url TEXT,
    duration_seconds INTEGER,
    status TEXT NOT NULL DEFAULT 'pending',
    status_reason TEXT,
    approved_at TIMESTAMPTZ,
    approved_by TEXT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_release_queue_status ON release_queue (status);
CREATE INDEX idx_release_queue_anilist ON release_queue (anilist_id);
CREATE INDEX idx_release_queue_updated ON release_queue (updated_at DESC);

CREATE TABLE anilist_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_key TEXT NOT NULL UNIQUE,
    anilist_id INTEGER,
    payload JSONB,
    hit_count INTEGER NOT NULL DEFAULT 0,
    last_hit_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

CREATE INDEX idx_anilist_cache_anilist_id ON anilist_cache (anilist_id);
CREATE INDEX idx_anilist_cache_expires ON anilist_cache (expires_at);

CREATE TABLE rss_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    release_group_filter TEXT,
    quality_filter TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_polled_at TIMESTAMPTZ,
    last_poll_items INTEGER,
    last_poll_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE seen_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    infohash TEXT UNIQUE,
    guid TEXT,
    source_id UUID REFERENCES rss_sources (id) ON DELETE SET NULL,
    release_name TEXT,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    release_queue_id UUID REFERENCES release_queue (id) ON DELETE SET NULL
);

CREATE INDEX idx_seen_releases_guid ON seen_releases (guid);
CREATE INDEX idx_seen_releases_first_seen ON seen_releases (first_seen_at DESC);

CREATE TABLE webhook_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    release_queue_id UUID REFERENCES release_queue (id) ON DELETE SET NULL,
    endpoint TEXT NOT NULL,
    request_body JSONB,
    response_status INTEGER,
    response_body TEXT,
    error TEXT,
    attempt INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_log_queue ON webhook_log (release_queue_id);
CREATE INDEX idx_webhook_log_created ON webhook_log (created_at DESC);
