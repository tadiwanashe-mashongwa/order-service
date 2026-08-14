ALTER TABLE outbox_events
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT;
