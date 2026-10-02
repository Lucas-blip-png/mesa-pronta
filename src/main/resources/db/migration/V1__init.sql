CREATE TABLE session (
    id           BIGSERIAL PRIMARY KEY,
    guild_id     VARCHAR(32)  NOT NULL,
    channel_id   VARCHAR(32)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    starts_at    TIMESTAMPTZ  NOT NULL,
    max_players  INT          NOT NULL CHECK (max_players BETWEEN 1 AND 50),
    created_by   VARCHAR(32)  NOT NULL,
    reminded_24h BOOLEAN      NOT NULL DEFAULT FALSE,
    reminded_1h  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_session_pending_reminders ON session (starts_at)
    WHERE NOT reminded_1h;

CREATE TABLE attendance (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT      NOT NULL REFERENCES session (id) ON DELETE CASCADE,
    user_id    VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_attendance_session_user UNIQUE (session_id, user_id)
);
