package com.studyflow.discipline;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DisciplineRepository {

  private final JdbcClient jdbc;

  public DisciplineRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<Discipline> findAll() {
    return jdbc.sql("SELECT id, name, color, icon, weight FROM disciplines ORDER BY name")
        .query(Discipline.class)
        .list();
  }

  public Optional<Discipline> findById(long id) {
    return jdbc.sql("SELECT id, name, color, icon, weight FROM disciplines WHERE id = :id")
        .param("id", id)
        .query(Discipline.class)
        .optional();
  }

  public Discipline insert(DisciplineRequest request) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(
            "INSERT INTO disciplines (name, color, icon, weight)"
                + " VALUES (:name, :color, :icon, :weight)")
        .param("name", request.name().trim())
        .param("color", request.color())
        .param("icon", request.icon())
        .param("weight", request.weight())
        .update(keys);
    return findById(keys.getKey().longValue()).orElseThrow();
  }

  public boolean update(long id, DisciplineRequest request) {
    return jdbc.sql(
                "UPDATE disciplines SET name = :name, color = :color, icon = :icon,"
                    + " weight = :weight WHERE id = :id")
            .param("name", request.name().trim())
            .param("color", request.color())
            .param("icon", request.icon())
            .param("weight", request.weight())
            .param("id", id)
            .update()
        > 0;
  }

  public boolean delete(long id) {
    return jdbc.sql("DELETE FROM disciplines WHERE id = :id").param("id", id).update() > 0;
  }
}
