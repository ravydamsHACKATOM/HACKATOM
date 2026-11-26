
package com.example.studyFlow.repository;

import com.example.studyFlow.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Busca disponibilidades de um dia específico da semana
    List<Availability> findByDayOfWeek(Integer dayOfWeek);

    // Lista as disponibilidades organizadas pelo dia da semana
    List<Availability> findAllByOrderByDayOfWeekAsc();
}
