
package com.example.studyFlow.service;

import com.example.studyFlow.controller.StudyFlowController.SubjectDTO;
import com.example.studyFlow.controller.StudyFlowController.EventDTO;
import com.example.studyFlow.controller.StudyFlowController.AvailabilityDTO;
import com.example.studyFlow.controller.StudyFlowController.StudyBlockDTO;

import com.example.studyFlow.entity.Subject;
import com.example.studyFlow.entity.StudyEvent;
import com.example.studyFlow.entity.Availability;
import com.example.studyFlow.entity.StudyBlock;

import com.example.studyFlow.repository.SubjectRepository;
import com.example.studyFlow.repository.StudyEventRepository;
import com.example.studyFlow.repository.AvailabilityRepository;
import com.example.studyFlow.repository.StudyBlockRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class StudyFlowService {

    private final SubjectRepository subjectRepository;
    private final StudyEventRepository eventRepository;
    private final AvailabilityRepository availabilityRepository;
    private final StudyBlockRepository blockRepository;

    public StudyFlowService(
            SubjectRepository subjectRepository,
            StudyEventRepository eventRepository,
            AvailabilityRepository availabilityRepository,
            StudyBlockRepository blockRepository
    ) {
        this.subjectRepository = subjectRepository;
        this.eventRepository = eventRepository;
        this.availabilityRepository = availabilityRepository;
        this.blockRepository = blockRepository;
    }

    // =========================
    // MATÉRIAS
    // =========================

    public List<SubjectDTO> getSubjects() {
        return subjectRepository.findAll()
                .stream()
                .map(this::toSubjectDTO)
                .toList();
    }

    public SubjectDTO createSubject(SubjectDTO dto) {

        if (dto.name() == null || dto.name().isBlank()) {
            throw badRequest("O nome da matéria é obrigatório.");
        }

        Subject subject = new Subject(
                dto.name(),
                dto.color()
        );

        return toSubjectDTO(subjectRepository.save(subject));
    }

    public SubjectDTO updateSubject(Long id, SubjectDTO dto) {

        Subject subject = findSubject(id);

        subject.setName(dto.name());
        subject.setColor(dto.color());

        return toSubjectDTO(subjectRepository.save(subject));
    }

    public void deleteSubject(Long id) {

        Subject subject = findSubject(id);

        List<StudyEvent> events =
                eventRepository.findBySubjectId(id);

        for (StudyEvent event : events) {
            List<StudyBlock> blocks =
                    blockRepository.findByEventId(event.getId());

            blockRepository.deleteAll(blocks);
        }

        eventRepository.deleteAll(events);
        subjectRepository.delete(subject);
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> notFound("Matéria não encontrada."));
    }

    private SubjectDTO toSubjectDTO(Subject subject) {
        return new SubjectDTO(
                subject.getId(),
                subject.getName(),
                subject.getColor()
        );
    }

    // =========================
    // PROVAS E ENTREGAS
    // =========================

    public List<EventDTO> getEvents() {
        return eventRepository.findAllByOrderByDueDateAsc()
                .stream()
                .map(this::toEventDTO)
                .toList();
    }

    public EventDTO createEvent(EventDTO dto) {

        validateEvent(dto);

        Subject subject = findSubject(dto.subjectId());

        StudyEvent event = new StudyEvent(
                dto.title(),
                dto.type(),
                subject,
                dto.dueDate(),
                dto.estimatedHours()
        );

        return toEventDTO(eventRepository.save(event));
    }

    public EventDTO updateEvent(Long id, EventDTO dto) {

        validateEvent(dto);

        StudyEvent event = findEvent(id);
        Subject subject = findSubject(dto.subjectId());

        event.setTitle(dto.title());
        event.setType(dto.type());
        event.setSubject(subject);
        event.setDueDate(dto.dueDate());
        event.setEstimatedHours(dto.estimatedHours());

        // Remove somente os blocos pendentes.
        List<StudyBlock> blocks =
                blockRepository.findByEventId(id);

        blockRepository.deleteAll(
                blocks.stream()
                        .filter(block -> !Boolean.TRUE.equals(block.getCompleted()))
                        .toList()
        );

        return toEventDTO(eventRepository.save(event));
    }

    public void deleteEvent(Long id) {

        StudyEvent event = findEvent(id);

        List<StudyBlock> blocks =
                blockRepository.findByEventId(id);

        blockRepository.deleteAll(blocks);
        eventRepository.delete(event);
    }

    private StudyEvent findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> notFound("Evento não encontrado."));
    }

    private EventDTO toEventDTO(StudyEvent event) {
        return new EventDTO(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getSubject().getId(),
                event.getDueDate(),
                event.getEstimatedHours()
        );
    }

    private void validateEvent(EventDTO dto) {

        if (dto.title() == null || dto.title().isBlank()) {
            throw badRequest("O título é obrigatório.");
        }

        if (dto.type() == null || dto.type().isBlank()) {
            throw badRequest("Informe o tipo do evento.");
        }

        if (dto.subjectId() == null) {
            throw badRequest("Selecione uma matéria.");
        }

        if (dto.dueDate() == null ||
                dto.dueDate().isBefore(LocalDate.now())) {
            throw badRequest("Informe uma data válida.");
        }

        if (dto.estimatedHours() == null ||
                dto.estimatedHours() <= 0) {
            throw badRequest("A estimativa deve ser maior que zero.");
        }
    }

    // =========================
    // DISPONIBILIDADE
    // =========================

    public List<AvailabilityDTO> getAvailability() {

        List<Availability> all =
                availabilityRepository.findAllByOrderByDayOfWeekAsc();

        List<AvailabilityDTO> result = new ArrayList<>();

        for (Availability availability : all) {
            result.add(new AvailabilityDTO(
                    availability.getId(),
                    List.of(availability.getDayOfWeek()),
                    availability.getStartTime(),
                    availability.getEndTime()
            ));
        }

        return result;
    }

    public AvailabilityDTO createAvailability(AvailabilityDTO dto) {

        validateAvailability(dto);

        List<Availability> saved = new ArrayList<>();

        for (Integer day : dto.daysOfWeek()) {
            Availability availability = new Availability(
                    day,
                    dto.startTime(),
                    dto.endTime()
            );

            saved.add(availabilityRepository.save(availability));
        }

        return new AvailabilityDTO(
                saved.get(0).getId(),
                dto.daysOfWeek(),
                dto.startTime(),
                dto.endTime()
        );
    }

    public AvailabilityDTO updateAvailability(
            Long id,
            AvailabilityDTO dto
    ) {

        validateAvailability(dto);

        Availability original = findAvailability(id);

        List<Availability> related =
                availabilityRepository.findAll().stream()
                        .filter(a ->
                                a.getStartTime().equals(original.getStartTime())
                                && a.getEndTime().equals(original.getEndTime()))
                        .toList();

        availabilityRepository.deleteAll(related);

        return createAvailability(dto);
    }

    public void deleteAvailability(Long id) {

        Availability original = findAvailability(id);

        List<Availability> related =
                availabilityRepository.findAll().stream()
                        .filter(a ->
                                a.getStartTime().equals(original.getStartTime())
                                && a.getEndTime().equals(original.getEndTime()))
                        .toList();

        availabilityRepository.deleteAll(related);
    }

    private Availability findAvailability(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Disponibilidade não encontrada."));
    }

    private void validateAvailability(AvailabilityDTO dto) {

        if (dto.daysOfWeek() == null ||
                dto.daysOfWeek().isEmpty()) {
            throw badRequest("Selecione pelo menos um dia.");
        }

        if (dto.startTime() == null ||
                dto.endTime() == null ||
                !dto.startTime().isBefore(dto.endTime())) {
            throw badRequest("Informe horários válidos.");
        }

        boolean invalidDay = dto.daysOfWeek().stream()
                .anyMatch(day -> day == null || day < 1 || day > 7);

        if (invalidDay) {
            throw badRequest("Os dias devem estar entre 1 e 7.");
        }
    }

    // =========================
    // BLOCOS DE ESTUDO
    // =========================

    public List<StudyBlockDTO> getStudyBlocks() {
        return blockRepository.findAllByOrderByStartAtAsc()
                .stream()
                .map(this::toBlockDTO)
                .toList();
    }

    public List<StudyBlockDTO> generateSchedule() {

        // Mantém os blocos concluídos e remove os pendentes antigos.
        List<StudyBlock> pending =
                blockRepository.findByCompleted(false);

        blockRepository.deleteAll(pending);

        List<StudyEvent> events =
                eventRepository.findAllByOrderByDueDateAsc();

        List<Availability> availabilities =
                availabilityRepository.findAllByOrderByDayOfWeekAsc();

        for (StudyEvent event : events) {

            long completedMinutes =
                    blockRepository.findByEventId(event.getId())
                            .stream()
                            .filter(block ->
                                    Boolean.TRUE.equals(block.getCompleted()))
                            .mapToLong(block ->
                                    Duration.between(
                                            block.getStartAt(),
                                            block.getEndAt()
                                    ).toMinutes())
                            .sum();

            long totalMinutes =
                    (long) (event.getEstimatedHours() * 60);

            long remaining = totalMinutes - completedMinutes;

            if (remaining <= 0) {
                continue;
            }

            LocalDate today = LocalDate.now();

            for (LocalDate date = today;
                 date.isBefore(event.getDueDate()) && remaining > 0;
                 date = date.plusDays(1)) {

                int day = date.getDayOfWeek().getValue();

                for (Availability availability : availabilities) {

                    if (availability.getDayOfWeek() != day) {
                        continue;
                    }

                    LocalTime start = availability.getStartTime();
                    LocalTime end = availability.getEndTime();

                    while (!start.plusMinutes(50).isAfter(end)
                            && remaining > 0) {

                        LocalDateTime startAt =
                                LocalDateTime.of(date, start);

                        LocalDateTime endAt =
                                startAt.plusMinutes(50);

                        if (!hasConflict(startAt, endAt)) {

                            StudyBlock block = new StudyBlock(
                                    event,
                                    startAt,
                                    endAt,
                                    false
                            );

                            blockRepository.save(block);
                            remaining -= 50;
                        }

                        // Sessão de 50 minutos + intervalo de 10 minutos.
                        start = start.plusMinutes(60);
                    }
                }
            }
        }

        return getStudyBlocks();
    }

    private boolean hasConflict(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {

        return blockRepository.findAll().stream()
                .anyMatch(block ->
                        startAt.isBefore(block.getEndAt())
                        && endAt.isAfter(block.getStartAt())
                );
    }

    private StudyBlockDTO toBlockDTO(StudyBlock block) {
        return new StudyBlockDTO(
                block.getId(),
                block.getEvent().getId(),
                block.getEvent().getTitle(),
                block.getStartAt(),
                block.getEndAt(),
                block.getCompleted()
        );
    }

    // =========================
    // CONCLUIR BLOCO
    // =========================

    public StudyBlockDTO completeStudyBlock(Long id) {

        StudyBlock block = blockRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Bloco de estudo não encontrado."));

        block.setCompleted(true);

        return toBlockDTO(blockRepository.save(block));
    }

    // =========================
    // TAREFAS DE HOJE
    // =========================

    public List<StudyBlockDTO> getTodayTasks() {

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return blockRepository.findByStartAtBetween(
                        start,
                        end.minusNanos(1)
                )
                .stream()
                .sorted(Comparator.comparing(StudyBlock::getStartAt))
                .map(this::toBlockDTO)
                .toList();
    }

    // =========================
    // RESPOSTAS DE ERRO
    // =========================

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
