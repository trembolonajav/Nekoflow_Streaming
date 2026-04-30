CREATE TABLE anime_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    votes INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE comment_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    reason VARCHAR(60) NOT NULL,
    report_count INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_reports_comment_id ON comment_reports (comment_id);
CREATE INDEX idx_comment_reports_status ON comment_reports (status);

INSERT INTO anime_suggestions (id, title, votes, status, note, created_at)
VALUES
    ('88888888-8888-8888-8888-888888888881', 'Sousou no Frieren', 1243, 'NEW', NULL, NOW() - INTERVAL '8 days'),
    ('88888888-8888-8888-8888-888888888882', 'Oshi no Ko 2ª Temporada', 987, 'IN_REVIEW', NULL, NOW() - INTERVAL '6 days'),
    ('88888888-8888-8888-8888-888888888883', 'Solo Leveling', 823, 'APPROVED', NULL, NOW() - INTERVAL '14 days'),
    ('88888888-8888-8888-8888-888888888884', 'Kaiju No. 8', 612, 'IN_REVIEW', NULL, NOW() - INTERVAL '13 days'),
    ('88888888-8888-8888-8888-888888888885', 'Violet Evergarden', 521, 'NEW', NULL, NOW() - INTERVAL '21 days'),
    ('88888888-8888-8888-8888-888888888886', 'Mushoku Tensei', 487, 'REJECTED', 'Já existem direitos exclusivos com outro distribuidor.', NOW() - INTERVAL '30 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO comment_reports (id, comment_id, reason, report_count, status, created_at)
SELECT
    '88888888-8888-8888-8888-888888888887',
    '77777777-7777-7777-7777-777777777774',
    'SPOILER',
    4,
    'PENDING',
    NOW() - INTERVAL '2 hours'
WHERE EXISTS (
    SELECT 1
    FROM comments
    WHERE id = '77777777-7777-7777-7777-777777777774'
)
ON CONFLICT (id) DO NOTHING;
