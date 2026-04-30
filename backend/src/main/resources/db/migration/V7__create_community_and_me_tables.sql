CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anime_id UUID NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id UUID REFERENCES comments (id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    contains_spoiler BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_episode_id ON comments (episode_id);
CREATE INDEX idx_comments_anime_id ON comments (anime_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);
CREATE INDEX idx_comments_user_id ON comments (user_id);

CREATE TABLE watch_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    anime_id UUID NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    progress_seconds INTEGER NOT NULL DEFAULT 0,
    progress_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    last_watched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watch_progress_user_anime UNIQUE (user_id, anime_id)
);

CREATE INDEX idx_watch_progress_user_id ON watch_progress (user_id);
CREATE INDEX idx_watch_progress_episode_id ON watch_progress (episode_id);

CREATE TABLE watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    anime_id UUID NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    watched_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_watch_history_user_id ON watch_history (user_id);
CREATE INDEX idx_watch_history_watched_at ON watch_history (watched_at DESC);

CREATE TABLE watchlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    anime_id UUID NOT NULL REFERENCES anime (id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'WATCHING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watchlist_user_anime UNIQUE (user_id, anime_id)
);

CREATE INDEX idx_watchlist_user_id ON watchlist (user_id);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    autoplay BOOLEAN NOT NULL DEFAULT TRUE,
    auto_next BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_audio VARCHAR(20) NOT NULL DEFAULT 'ja',
    preferred_subtitle VARCHAR(20) NOT NULL DEFAULT 'pt-BR',
    preferred_quality VARCHAR(20) NOT NULL DEFAULT 'auto',
    notify_releases BOOLEAN NOT NULL DEFAULT TRUE,
    notify_new_episodes BOOLEAN NOT NULL DEFAULT TRUE,
    notify_watchlist BOOLEAN NOT NULL DEFAULT TRUE,
    notify_marketing BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    admin_user_id UUID;
BEGIN
    SELECT id INTO admin_user_id
    FROM users
    WHERE lower(email) = 'admin@nekoflow.app'
    LIMIT 1;

    IF admin_user_id IS NOT NULL THEN
        INSERT INTO user_preferences (user_id)
        VALUES (admin_user_id)
        ON CONFLICT (user_id) DO NOTHING;

        INSERT INTO watchlist (id, user_id, anime_id, status)
        VALUES (
            '77777777-7777-7777-7777-777777777771',
            admin_user_id,
            '11111111-1111-1111-1111-111111111111',
            'WATCHING'
        )
        ON CONFLICT (user_id, anime_id) DO NOTHING;

        INSERT INTO watch_progress (
            id, user_id, anime_id, episode_id, progress_seconds, progress_percent, last_watched_at
        ) VALUES (
            '77777777-7777-7777-7777-777777777772',
            admin_user_id,
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222221',
            840,
            58.33,
            NOW()
        )
        ON CONFLICT (user_id, anime_id) DO NOTHING;

        INSERT INTO watch_history (id, user_id, anime_id, episode_id, watched_at)
        VALUES (
            '77777777-7777-7777-7777-777777777773',
            admin_user_id,
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222221',
            NOW() - INTERVAL '2 hours'
        )
        ON CONFLICT DO NOTHING;

        INSERT INTO comments (
            id, anime_id, episode_id, user_id, parent_id, body, contains_spoiler, status
        ) VALUES (
            '77777777-7777-7777-7777-777777777774',
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222221',
            admin_user_id,
            NULL,
            'Primeira impressão muito forte. A adaptação segurou bem o ritmo e o clima.',
            FALSE,
            'VISIBLE'
        )
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;
