CREATE TABLE IF NOT EXISTS subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    color VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS study_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(150) NOT NULL CHECK (length(trim(title)) > 0),
    type VARCHAR(30) NOT NULL,
    subject_id INTEGER NOT NULL,
    due_date DATE NOT NULL,
    estimated_hours DOUBLE NOT NULL CHECK (estimated_hours > 0 AND estimated_hours <= 100),
    CONSTRAINT fk_study_events_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS study_blocks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE CHECK (completed IN (0, 1)),
    CONSTRAINT fk_study_blocks_event FOREIGN KEY (event_id) REFERENCES study_events(id) ON DELETE CASCADE,
    CHECK (end_at > start_at)
);

CREATE TABLE IF NOT EXISTS availabilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CHECK (end_time > start_time),
    CONSTRAINT uq_availability_day_interval UNIQUE (day_of_week, start_time, end_time)
);

CREATE INDEX IF NOT EXISTS idx_events_due_date ON study_events(due_date);
CREATE INDEX IF NOT EXISTS idx_events_subject_id ON study_events(subject_id);
CREATE INDEX IF NOT EXISTS idx_blocks_event_id ON study_blocks(event_id);
CREATE INDEX IF NOT EXISTS idx_blocks_start_at ON study_blocks(start_at);
CREATE INDEX IF NOT EXISTS idx_availability_day ON availabilities(day_of_week);
