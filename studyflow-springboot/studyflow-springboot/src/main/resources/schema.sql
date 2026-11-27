CREATE TABLE IF NOT EXISTS disciplines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    color TEXT NOT NULL DEFAULT '#7C6CF2',
    description TEXT
);

CREATE TABLE IF NOT EXISTS study_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    event_type TEXT NOT NULL CHECK (event_type IN ('EXAM', 'ASSIGNMENT')),
    due_date TEXT NOT NULL,
    estimated_hours REAL NOT NULL,
    discipline_id INTEGER NOT NULL,
    description TEXT,
    FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS availabilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS study_blocks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    study_event_id INTEGER NOT NULL,
    discipline_id INTEGER NOT NULL,
    study_date TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (study_event_id) REFERENCES study_events(id) ON DELETE CASCADE,
    FOREIGN KEY (discipline_id) REFERENCES disciplines(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_due_date ON study_events(due_date);
CREATE INDEX IF NOT EXISTS idx_blocks_date ON study_blocks(study_date);
