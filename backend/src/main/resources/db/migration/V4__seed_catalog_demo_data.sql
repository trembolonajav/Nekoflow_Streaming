DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM anime WHERE id = '11111111-1111-1111-1111-111111111111') THEN
        INSERT INTO anime (
            id, slug, anilist_id, title_display, title_romaji, title_native, title_english,
            synopsis, type, status, season_label, year, visibility, cover_url, banner_url,
            average_score, studio, published_at
        ) VALUES (
            '11111111-1111-1111-1111-111111111111',
            'frieren-beyond-journeys-end',
            154587,
            'Frieren: Beyond Journey''s End',
            'Sousou no Frieren',
            '葬送のフリーレン',
            'Frieren: Beyond Journey''s End',
            'Apos derrotar o Rei Demonio, Frieren parte em uma jornada marcada por memoria, tempo e reencontro.',
            'SERIES',
            'RELEASING',
            'Winter 2024',
            2024,
            'PUBLISHED',
            'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg',
            'https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587.jpg',
            91.40,
            'Madhouse',
            NOW()
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM episodes WHERE id = '22222222-2222-2222-2222-222222222221') THEN
        INSERT INTO episodes (
            id, anime_id, number, title, summary, duration_seconds, thumbnail_url, status, published_at
        ) VALUES (
            '22222222-2222-2222-2222-222222222221',
            '11111111-1111-1111-1111-111111111111',
            1,
            'The Journey''s End',
            'Primeiro episodio publicado no catalogo da plataforma.',
            1440,
            'https://img.anili.st/media/154587',
            'PUBLISHED',
            NOW()
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM episodes WHERE id = '22222222-2222-2222-2222-222222222222') THEN
        INSERT INTO episodes (
            id, anime_id, number, title, summary, duration_seconds, thumbnail_url, status, published_at
        ) VALUES (
            '22222222-2222-2222-2222-222222222222',
            '11111111-1111-1111-1111-111111111111',
            2,
            'It Didn''t Have to Be Magic',
            'Segundo episodio publicado para validar listagem e navegacao.',
            1440,
            'https://img.anili.st/media/154587?ep=2',
            'PUBLISHED',
            NOW()
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM episode_video_sources WHERE id = '33333333-3333-3333-3333-333333333331') THEN
        INSERT INTO episode_video_sources (
            id, episode_id, provider, external_video_id, embed_url, player_url, is_default, status
        ) VALUES (
            '33333333-3333-3333-3333-333333333331',
            '22222222-2222-2222-2222-222222222221',
            'SEEKSTREAMING',
            'frieren-ep-1',
            'https://seekstreaming.com/embed/frieren-ep-1',
            'https://seekstreaming.com/player/frieren-ep-1',
            TRUE,
            'ACTIVE'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM home_section_items WHERE id = '44444444-4444-4444-4444-444444444441') THEN
        INSERT INTO home_section_items (
            id, section_id, anime_id, sort_order, is_pinned
        )
        SELECT
            '44444444-4444-4444-4444-444444444441',
            id,
            '11111111-1111-1111-1111-111111111111',
            1,
            TRUE
        FROM home_sections
        WHERE code = 'continue';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM home_section_items WHERE id = '44444444-4444-4444-4444-444444444442') THEN
        INSERT INTO home_section_items (
            id, section_id, anime_id, sort_order, is_pinned
        )
        SELECT
            '44444444-4444-4444-4444-444444444442',
            id,
            '11111111-1111-1111-1111-111111111111',
            1,
            TRUE
        FROM home_sections
        WHERE code = 'season';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM home_section_items WHERE id = '44444444-4444-4444-4444-444444444443') THEN
        INSERT INTO home_section_items (
            id, section_id, episode_id, sort_order, is_pinned
        )
        SELECT
            '44444444-4444-4444-4444-444444444443',
            id,
            '22222222-2222-2222-2222-222222222221',
            1,
            TRUE
        FROM home_sections
        WHERE code = 'recent';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM hero_config WHERE id = '55555555-5555-5555-5555-555555555551') THEN
        INSERT INTO hero_config (
            id, anime_id, headline, subheadline, cta_label, is_active
        ) VALUES (
            '55555555-5555-5555-5555-555555555551',
            '11111111-1111-1111-1111-111111111111',
            'Destaque editorial',
            'Uma jornada sobre tempo, memoria e silencio.',
            'Assistir agora',
            TRUE
        );
    END IF;
END $$;
