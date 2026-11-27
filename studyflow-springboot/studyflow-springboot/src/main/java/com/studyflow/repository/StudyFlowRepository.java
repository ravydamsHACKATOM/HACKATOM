package com.studyflow.repository;

import com.studyflow.model.*;
import com.studyflow.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StudyFlowRepository {
    private final JdbcTemplate jdbc;

    public StudyFlowRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Discipline> disciplineMapper = (rs, n) ->
            new Discipline(rs.getLong("id"), rs.getString("name"),
                    rs.getString("color"), rs.getString("description"));

    private final RowMapper<StudyEvent> eventMapper = (rs, n) ->
            new StudyEvent(rs.getLong("id"), rs.getString("title"),
                    rs.getString("event_type"), rs.getString("due_date"),
                    rs.getDouble("estimated_hours"), rs.getLong("discipline_id"),
                    rs.getString("description"));

    private final RowMapper<Availability> availabilityMapper = (rs, n) ->
            new Availability(rs.getLong("id"), rs.getInt("day_of_week"),
                    rs.getString("start_time"), rs.getString("end_time"));

    private final RowMapper<StudyBlock> blockMapper = (rs, n) ->
            new StudyBlock(rs.getLong("id"), rs.getLong("study_event_id"),
                    rs.getLong("discipline_id"), rs.getString("study_date"),
                    rs.getString("start_time"), rs.getString("end_time"),
                    rs.getInt("duration_minutes"), rs.getInt("completed") == 1);

    public List<Discipline> findAllDisciplines() {
        return jdbc.query("SELECT * FROM disciplines ORDER BY name", disciplineMapper);
    }

    public Optional<Discipline> findDiscipline(long id) {
        return jdbc.query("SELECT * FROM disciplines WHERE id = ?", disciplineMapper, id)
                .stream().findFirst();
    }

    public Discipline createDiscipline(DisciplineRequest r) {
        jdbc.update("INSERT INTO disciplines(name,color,description) VALUES(?,?,?)",
                r.name(), defaultColor(r.color()), r.description());
        return findDiscipline(lastId()).orElseThrow();
    }

    public Optional<Discipline> updateDiscipline(long id, DisciplineRequest r) {
        int changed = jdbc.update("UPDATE disciplines SET name=?,color=?,description=? WHERE id=?",
                r.name(), defaultColor(r.color()), r.description(), id);
        return changed == 0 ? Optional.empty() : findDiscipline(id);
    }

    public boolean deleteDiscipline(long id) {
        return jdbc.update("DELETE FROM disciplines WHERE id=?", id) > 0;
    }

    public List<StudyEvent> findAllEvents() {
        return jdbc.query("SELECT * FROM study_events ORDER BY due_date", eventMapper);
    }

    public Optional<StudyEvent> findEvent(long id) {
        return jdbc.query("SELECT * FROM study_events WHERE id=?", eventMapper, id)
                .stream().findFirst();
    }

    public StudyEvent createEvent(StudyEventRequest r) {
        jdbc.update("INSERT INTO study_events(title,event_type,due_date,estimated_hours,discipline_id,description) VALUES(?,?,?,?,?,?)",
                r.title(), r.eventType(), r.dueDate().toString(), r.estimatedHours(),
                r.disciplineId(), r.description());
        return findEvent(lastId()).orElseThrow();
    }

    public Optional<StudyEvent> updateEvent(long id, StudyEventRequest r) {
        int changed = jdbc.update("UPDATE study_events SET title=?,event_type=?,due_date=?,estimated_hours=?,discipline_id=?,description=? WHERE id=?",
                r.title(), r.eventType(), r.dueDate().toString(), r.estimatedHours(),
                r.disciplineId(), r.description(), id);
        return changed == 0 ? Optional.empty() : findEvent(id);
    }

    public boolean deleteEvent(long id) {
        return jdbc.update("DELETE FROM study_events WHERE id=?", id) > 0;
    }

    public List<Availability> findAllAvailabilities() {
        return jdbc.query("SELECT * FROM availabilities ORDER BY day_of_week,start_time", availabilityMapper);
    }

    public Optional<Availability> findAvailability(long id) {
        return jdbc.query("SELECT * FROM availabilities WHERE id=?", availabilityMapper, id)
                .stream().findFirst();
    }

    public Availability createAvailability(AvailabilityRequest r) {
        jdbc.update("INSERT INTO availabilities(day_of_week,start_time,end_time) VALUES(?,?,?)",
                r.dayOfWeek(), r.startTime().toString(), r.endTime().toString());
        return findAvailability(lastId()).orElseThrow();
    }

    public Optional<Availability> updateAvailability(long id, AvailabilityRequest r) {
        int changed = jdbc.update("UPDATE availabilities SET day_of_week=?,start_time=?,end_time=? WHERE id=?",
                r.dayOfWeek(), r.startTime().toString(), r.endTime().toString(), id);
        return changed == 0 ? Optional.empty() : findAvailability(id);
    }

    public boolean deleteAvailability(long id) {
        return jdbc.update("DELETE FROM availabilities WHERE id=?", id) > 0;
    }

    public List<StudyBlock> findBlocks(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT * FROM study_blocks WHERE study_date BETWEEN ? AND ? ORDER BY study_date,start_time",
                blockMapper, from.toString(), to.toString());
    }

    public List<StudyBlock> findBlocksForDay(LocalDate date) {
        return jdbc.query("SELECT * FROM study_blocks WHERE study_date=? ORDER BY start_time",
                blockMapper, date.toString());
    }

    public Optional<StudyBlock> findBlock(long id) {
        return jdbc.query("SELECT * FROM study_blocks WHERE id=?", blockMapper, id)
                .stream().findFirst();
    }

    public List<StudyEvent> findEventsDueBy(LocalDate date) {
        return jdbc.query("SELECT * FROM study_events WHERE due_date >= ? ORDER BY due_date",
                eventMapper, date.toString());
    }

    public void deletePendingBlocksBetween(LocalDate from, LocalDate to) {
        jdbc.update("DELETE FROM study_blocks WHERE study_date BETWEEN ? AND ? AND completed=0",
                from.toString(), to.toString());
    }

    public boolean hasOverlap(LocalDate date, LocalTime start, LocalTime end) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM study_blocks WHERE study_date=? AND start_time < ? AND end_time > ?",
                Integer.class, date.toString(), end.toString(), start.toString());
        return count != null && count > 0;
    }

    public StudyBlock createBlock(long eventId, long disciplineId, LocalDate date,
                                  LocalTime start, LocalTime end, int minutes) {
        jdbc.update("INSERT INTO study_blocks(study_event_id,discipline_id,study_date,start_time,end_time,duration_minutes,completed) VALUES(?,?,?,?,?,?,0)",
                eventId, disciplineId, date.toString(), start.toString(), end.toString(), minutes);
        return findBlock(lastId()).orElseThrow();
    }

    public Optional<StudyBlock> setBlockCompleted(long id, boolean completed) {
        int changed = jdbc.update("UPDATE study_blocks SET completed=? WHERE id=?",
                completed ? 1 : 0, id);
        return changed == 0 ? Optional.empty() : findBlock(id);
    }

    public Optional<StudyBlock> updateBlockTime(long id, LocalDate date, LocalTime start, LocalTime end) {
        int changed = jdbc.update("UPDATE study_blocks SET study_date=?,start_time=?,end_time=?,duration_minutes=? WHERE id=?",
                date.toString(), start.toString(), end.toString(),
                (int) java.time.Duration.between(start, end).toMinutes(), id);
        return changed == 0 ? Optional.empty() : findBlock(id);
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private String defaultColor(String color) {
        return color == null || color.isBlank() ? "#7C6CF2" : color;
    }
}
