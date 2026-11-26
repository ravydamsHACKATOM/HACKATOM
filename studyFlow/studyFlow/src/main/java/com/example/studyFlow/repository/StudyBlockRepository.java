
package com.example.studyFlow.repository;

import com.example.studyFlow.entity.StudyBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyBlockRepository extends JpaRepository<StudyBlock, Long> {

    // Busca blocos relacionados a uma prova ou entrega
    List<StudyBlock> findByEventId(Long eventId);

    // Busca blocos concluídos ou pendentes
    List<StudyBlock> findByCompleted(Boolean completed);

    // Lista os blocos organizados pelo horário de início
    List<StudyBlock> findAllByOrderByStartAtAsc();

    // Busca blocos dentro de um período
    List<StudyBlock> findByStartAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}
