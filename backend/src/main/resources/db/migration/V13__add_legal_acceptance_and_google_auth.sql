CREATE TABLE user_terms_acceptance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    terms_version VARCHAR(40) NOT NULL,
    privacy_policy_version VARCHAR(40) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(64),
    user_agent TEXT,
    auth_provider VARCHAR(30) NOT NULL
);

CREATE INDEX idx_user_terms_acceptance_user_id ON user_terms_acceptance (user_id);
CREATE INDEX idx_user_terms_acceptance_accepted_at ON user_terms_acceptance (accepted_at DESC);

ALTER TABLE users
    ADD COLUMN google_sub VARCHAR(255);

CREATE UNIQUE INDEX uq_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;

UPDATE users
SET provider = 'LOCAL'
WHERE lower(provider) = 'email';
