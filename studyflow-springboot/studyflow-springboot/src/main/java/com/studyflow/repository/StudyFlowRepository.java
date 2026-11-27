package com.studyflow.repository;

import com.studyflow.dto.AvailabilityRequest;
import com.studyflow.dto.DisciplineRequest;
import com.studyflow.dto.StudyEventRequest;
import com.studyflow.model.Availability;
import com.studyflow.model.Discipline;
import com.studyflow.model.StudyBlock;
import com.studyflow.model.StudyEvent;
import com.studyflow.service.CurrentUser;
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
    private final CurrentUser currentUser;

    public StudyFlowRepository(JdbcTemplate jdbc, CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
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
        return jdbc.query("SELECT * FROM disciplines WHERE user_id=? ORDER BY name",
                disciplineMapper, currentUser.id());
    }

    public Optional<Discipline> findDiscipline(long id) {
        return jdbc.query("SELECT * FROM disciplines WHERE id=? AND user_id=?",
                        disciplineMapper, id, currentUser.id())
                .stream().findFirst();
    }

    public Discipline createDiscipline(DisciplineRequest r) {
        jdbc.update("INSERT INTO disciplines(user_id,name,color,description) VALUES(?,?,?,?)",
                currentUser.id(), r.name(), defaultColor(r.color()), r.description());
        return findDiscipline(lastId()).orElseThrow();
    }

    public Optional<Discipline> updateDiscipline(long id, DisciplineRequest r) {
        int changed = jdbc.update("UPDATE disciplines SET name=?,color=?,description=? WHERE id=? AND user_id=?",
                r.name(), defaultColor(r.color()), r.description(), id, currentUser.id());
        return changed == 0 ? Optional.empty() : findDiscipline(id);
    }

    public boolean deleteDiscipline(long id) {
        return jdbc.update("DELETE FROM disciplines WHERE id=? AND user_id=?", id,
                currentUser.id()) > 0;
    }

    public List<StudyEvent> findAllEvents() {
        return jdbc.query("SELECT * FROM study_events WHERE user_id=? ORDER BY due_date",
                eventMapper, currentUser.id());
    }

    public Optional<StudyEvent> findEvent(long id) {
        return jdbc.query("SELECT * FROM study_events WHERE id=? AND user_id=?", eventMapper,
                        id, currentUser.id())
                .stream().findFirst();
    }

    public StudyEvent createEvent(StudyEventRequest r) {
        jdbc.update("INSERT INTO study_events(user_id,title,event_type,due_date,estimated_hours,discipline_id,description) VALUES(?,?,?,?,?,?,?)",
                currentUser.id(), r.title(), r.eventType(), r.dueDate().toString(), r.estimatedHours(),
                r.disciplineId(), r.description());
        return findEvent(lastId()).orElseThrow();
    }

    public Optional<StudyEvent> updateEvent(long id, StudyEventRequest r) {
        int changed = jdbc.update("UPDATE study_events SET title=?,event_type=?,due_date=?,estimated_hours=?,discipline_id=?,description=? WHERE id=? AND user_id=?",
                r.title(), r.eventType(), r.dueDate().toString(), r.estimatedHours(),
                r.disciplineId(), r.description(), id, currentUser.id());
        return changed == 0 ? Optional.empty() : findEvent(id);
    }

    public boolean deleteEvent(long id) {
        return jdbc.update("DELETE FROM study_events WHERE id=? AND user_id=?", id,
                currentUser.id()) > 0;
    }

    public List<Availability> findAllAvailabilities() {
        return jdbc.query("SELECT * FROM availabilities WHERE user_id=? ORDER BY day_of_week,start_time",
                availabilityMapper, currentUser.id());
    }

    public Optional<Availability> findAvailability(long id) {
        return jdbc.query("SELECT * FROM availabilities WHERE id=? AND user_id=?", availabilityMapper,
                        id, currentUser.id())
                .stream().findFirst();
    }

    public Availability createAvailability(AvailabilityRequest r) {
        jdbc.update("INSERT INTO availabilities(user_id,day_of_week,start_time,end_time) VALUES(?,?,?,?)",
                currentUser.id(), r.dayOfWeek(), r.startTime().toString(), r.endTime().toString());
        return findAvailability(lastId()).orElseThrow();
    }

    public Optional<Availability> updateAvailability(long id, AvailabilityRequest r) {
        int changed = jdbc.update("UPDATE availabilities SET day_of_week=?,start_time=?,end_time=? WHERE id=? AND user_id=?",
                r.dayOfWeek(), r.startTime().toString(), r.endTime().toString(), id,
                currentUser.id());
        return changed == 0 ? Optional.empty() : findAvailability(id);
    }

    public boolean deleteAvailability(long id) {
        return jdbc.update("DELETE FROM availabilities WHERE id=? AND user_id=?", id,
                currentUser.id()) > 0;
    }

    public List<StudyBlock> findBlocks(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT * FROM study_blocks WHERE user_id=? AND study_date BETWEEN ? AND ? ORDER BY study_date,start_time",
                blockMapper, currentUser.id(), from.toString(), to.toString());
    }

    public List<StudyBlock> findBlocksForDay(LocalDate date) {
        return jdbc.query("SELECT * FROM study_blocks WHERE user_id=? AND study_date=? ORDER BY start_time",
                blockMapper, currentUser.id(), date.toString());
    }

    public Optional<StudyBlock> findBlock(long id) {
        return jdbc.query("SELECT * FROM study_blocks WHERE id=? AND user_id=?", blockMapper,
                        id, currentUser.id())
                .stream().findFirst();
    }

    public List<StudyEvent> findEventsDueBy(LocalDate date) {
        return jdbc.query("SELECT * FROM study_events WHERE user_id=? AND due_date >= ? ORDER BY due_date",
                eventMapper, currentUser.id(), date.toString());
    }

    public void deletePendingBlocksBetween(LocalDate from, LocalDate to) {
        jdbc.update("DELETE FROM study_blocks WHERE user_id=? AND study_date BETWEEN ? AND ? AND completed=0",
                currentUser.id(), from.toString(), to.toString());
    }

    public boolean hasOverlap(LocalDate date, LocalTime start, LocalTime end) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM study_blocks WHERE user_id=? AND study_date=? AND start_time < ? AND end_time > ?",
                Integer.class, currentUser.id(), date.toString(), end.toString(), start.toString());
        return count != null && count > 0;
    }

    public StudyBlock createBlock(long eventId, long disciplineId, LocalDate date,
                                  LocalTime start, LocalTime end, int minutes) {
        jdbc.update("INSERT INTO study_blocks(user_id,study_event_id,discipline_id,study_date,start_time,end_time,duration_minutes,completed) VALUES(?,?,?,?,?,?,?,0)",
                currentUser.id(), eventId, disciplineId, date.toString(), start.toString(), end.toString(), minutes);
        return findBlock(lastId()).orElseThrow();
    }

    public Optional<StudyBlock> setBlockCompleted(long id, boolean completed) {
        int changed = jdbc.update("UPDATE study_blocks SET completed=? WHERE id=? AND user_id=?",
                completed ? 1 : 0, id, currentUser.id());
        return changed == 0 ? Optional.empty() : findBlock(id);
    }

    public Optional<StudyBlock> updateBlockTime(long id, LocalDate date, LocalTime start, LocalTime end) {
        int changed = jdbc.update("UPDATE study_blocks SET study_date=?,start_time=?,end_time=?,duration_minutes=? WHERE id=? AND user_id=?",
                date.toString(), start.toString(), end.toString(),
                (int) java.time.Duration.between(start, end).toMinutes(), id, currentUser.id());
        return changed == 0 ? Optional.empty() : findBlock(id);
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private String defaultColor(String color) {
        return color == null || color.isBlank() ? "#7C6CF2" : color;
    }
}
