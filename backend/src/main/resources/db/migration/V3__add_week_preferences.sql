-- How the student wants a week organised: spread over the days and a chosen share per event.

CREATE TABLE week_preferences (
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start TEXT    NOT NULL,
    spread     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, week_start)
);

-- Minutes of study the student wants for one exam or assignment in one week.
CREATE TABLE week_allocations (
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start TEXT    NOT NULL,
    event_id   INTEGER NOT NULL REFERENCES study_events (id) ON DELETE CASCADE,
    minutes    INTEGER NOT NULL CHECK (minutes >= 0),
    PRIMARY KEY (user_id, week_start, event_id)
);
