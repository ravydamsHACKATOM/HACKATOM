-- Accounts: every discipline, availability window and plan now belongs to one user.

CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL
);

-- Server-side sessions. Only a SHA-256 digest of the cookie token is stored.
CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TEXT    NOT NULL
);

CREATE INDEX idx_sessions_user ON sessions (user_id);

-- Data created before accounts existed has no owner and cannot be attributed to anyone.
DELETE FROM study_blocks;
DELETE FROM study_events;
DELETE FROM disciplines;
DELETE FROM availability_windows;

ALTER TABLE disciplines ADD COLUMN user_id INTEGER REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE availability_windows ADD COLUMN user_id INTEGER REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_disciplines_user ON disciplines (user_id);
CREATE INDEX idx_availability_user ON availability_windows (user_id);

-- Plan notes and the data revision become per user.
DROP TABLE plan_notes;

CREATE TABLE plan_notes (
    user_id       INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start    TEXT    NOT NULL,
    summary       TEXT    NOT NULL,
    tips          TEXT    NOT NULL,
    created_at    TEXT    NOT NULL,
    data_revision INTEGER NOT NULL,
    PRIMARY KEY (user_id, week_start)
);

DROP TABLE data_revision;

CREATE TABLE data_revision (
    user_id  INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    revision INTEGER NOT NULL
);
