CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    avatar_url TEXT,
    provider VARCHAR(30) NOT NULL DEFAULT 'email',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE anime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(180) NOT NULL UNIQUE,
    anilist_id BIGINT,
    title_display VARCHAR(255) NOT NULL,
    title_romaji VARCHAR(255),
    title_native VARCHAR(255),
    title_english VARCHAR(255),
    synopsis TEXT,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    season_label VARCHAR(80),
    year INTEGER,
    visibility VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    cover_url TEXT,
    banner_url TEXT,
    average_score NUMERIC(5, 2),
    studio VARCHAR(255),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_anime_visibility ON anime (visibility);
CREATE INDEX idx_anime_anilist_id ON anime (anilist_id);

CREATE TABLE episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anime_id UUID NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    duration_seconds INTEGER,
    thumbnail_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    scheduled_for TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_episode_number_per_anime UNIQUE (anime_id, number)
);

CREATE INDEX idx_episodes_anime_id ON episodes (anime_id);
CREATE INDEX idx_episodes_status ON episodes (status);

CREATE TABLE episode_video_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    external_video_id VARCHAR(255),
    embed_url TEXT,
    player_url TEXT,
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_episode_video_sources_episode_id ON episode_video_sources (episode_id);

CREATE TABLE home_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE home_section_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES home_sections (id) ON DELETE CASCADE,
    anime_id UUID REFERENCES anime (id) ON DELETE CASCADE,
    episode_id UUID REFERENCES episodes (id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_home_item_target CHECK (
        (anime_id IS NOT NULL AND episode_id IS NULL)
        OR (anime_id IS NULL AND episode_id IS NOT NULL)
    )
);

CREATE INDEX idx_home_section_items_section_id ON home_section_items (section_id);

CREATE TABLE hero_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anime_id UUID REFERENCES anime (id) ON DELETE SET NULL,
    headline VARCHAR(255),
    subheadline TEXT,
    cta_label VARCHAR(80),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
