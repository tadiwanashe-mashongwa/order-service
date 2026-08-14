ALTER TABLE outbox_events
    ADD COLUMN dead_lettered BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_outbox_events_pending_retry
    ON outbox_events (published, dead_lettered, next_attempt_at, created_at);
