
package com.example.studyFlow.repository;

import com.example.studyFlow.entity.StudyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyEventRepository extends JpaRepository<StudyEvent, Long> {

    // Busca eventos relacionados a uma matéria
    List<StudyEvent> findBySubjectId(Long subjectId);

    // Busca eventos por tipo: PROVA ou ENTREGA
    List<StudyEvent> findByType(String type);

    // Lista eventos ordenados pela data de entrega
    List<StudyEvent> findAllByOrderByDueDateAsc();

    // Busca eventos com prazo até determinada data
    List<StudyEvent> findByDueDateLessThanEqual(LocalDate date);
}
