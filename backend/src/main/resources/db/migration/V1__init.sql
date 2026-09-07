CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL
);

CREATE TABLE tests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    language VARCHAR(20) NOT NULL,
    instructions_html TEXT NOT NULL,
    starter_code TEXT NOT NULL,
    ai_trap_phrase TEXT,
    duration_minutes INTEGER NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE test_assignments (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES tests(id),
    recruiter_id BIGINT NOT NULL REFERENCES users(id),
    candidate_name VARCHAR(255) NOT NULL,
    role_applied_for VARCHAR(255) NOT NULL,
    token UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    submitted_code TEXT,
    possible_ai_flag BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE admin_invites (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token UUID NOT NULL UNIQUE,
    invited_by BIGINT NOT NULL REFERENCES users(id),
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_test_assignments_recruiter ON test_assignments(recruiter_id);
CREATE INDEX idx_tests_active ON tests(active);
