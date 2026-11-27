CREATE TABLE disciplines (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    name   TEXT    NOT NULL,
    color  TEXT    NOT NULL,
    icon   TEXT    NOT NULL,
    weight INTEGER NOT NULL DEFAULT 2 CHECK (weight BETWEEN 1 AND 3)
);

CREATE TABLE study_events (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    discipline_id  INTEGER NOT NULL REFERENCES disciplines (id) ON DELETE CASCADE,
    title          TEXT    NOT NULL,
    type           TEXT    NOT NULL CHECK (type IN ('EXAM', 'ASSIGNMENT')),
    due_date       TEXT    NOT NULL,
    effort_minutes INTEGER NOT NULL CHECK (effort_minutes > 0),
    notes          TEXT
);

CREATE TABLE availability_windows (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  TEXT    NOT NULL,
    end_time    TEXT    NOT NULL,
    CHECK (start_time < end_time)
);

CREATE TABLE study_blocks (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id   INTEGER NOT NULL REFERENCES study_events (id) ON DELETE CASCADE,
    block_date TEXT    NOT NULL,
    start_time TEXT    NOT NULL,
    end_time   TEXT    NOT NULL,
    kind       TEXT    NOT NULL CHECK (kind IN ('FOCUS', 'REVIEW')),
    status     TEXT    NOT NULL CHECK (status IN ('PLANNED', 'DONE', 'SKIPPED', 'MISSED')),
    locked     INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_study_blocks_date ON study_blocks (block_date);
CREATE INDEX idx_study_blocks_event ON study_blocks (event_id);
CREATE INDEX idx_study_events_due ON study_events (due_date);

-- One advisor note per generated week, plus the data revision it was generated from.
CREATE TABLE plan_notes (
    week_start    TEXT PRIMARY KEY,
    summary       TEXT    NOT NULL,
    tips          TEXT    NOT NULL,
    source        TEXT    NOT NULL,
    created_at    TEXT    NOT NULL,
    data_revision INTEGER NOT NULL
);

-- Single-row counter bumped whenever events or availability change, so a plan can tell it is stale.
CREATE TABLE data_revision (
    id       INTEGER PRIMARY KEY CHECK (id = 1),
    revision INTEGER NOT NULL
);

INSERT INTO data_revision (id, revision) VALUES (1, 0);
