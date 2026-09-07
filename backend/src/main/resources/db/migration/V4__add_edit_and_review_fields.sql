ALTER TABLE test_assignments
    ADD COLUMN review_comment TEXT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN reviewed_by BIGINT REFERENCES users(id),
    ADD COLUMN proctoring_events TEXT;
