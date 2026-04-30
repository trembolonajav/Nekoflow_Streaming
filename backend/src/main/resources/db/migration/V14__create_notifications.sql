CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(30) NOT NULL DEFAULT 'INFO',
    target_type VARCHAR(40) NOT NULL,
    target_user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    target_role VARCHAR(40),
    related_entity_type VARCHAR(60),
    related_entity_id UUID,
    action_url VARCHAR(500),
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

CREATE TABLE notification_reads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_read_user UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notifications_target_user ON notifications (target_user_id, created_at DESC);
CREATE INDEX idx_notifications_target_role ON notifications (target_role, created_at DESC);
CREATE INDEX idx_notifications_target_type ON notifications (target_type, created_at DESC);
CREATE INDEX idx_notifications_created ON notifications (created_at DESC);
CREATE INDEX idx_notifications_expires ON notifications (expires_at);
CREATE INDEX idx_notification_reads_user ON notification_reads (user_id);
