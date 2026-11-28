package com.studyflow.discipline;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Every query is scoped by the owner, so one account can never read or change another's data. */
@Repository
public class DisciplineRepository {

  private static final String SELECT = "SELECT id, name, color, icon, weight FROM disciplines";

  private final JdbcClient jdbc;

  public DisciplineRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<Discipline> findAll(long userId) {
    return jdbc.sql(SELECT + " WHERE user_id = :userId ORDER BY name")
        .param("userId", userId)
        .query(Discipline.class)
        .list();
  }

  public Optional<Discipline> findById(long userId, long id) {
    return jdbc.sql(SELECT + " WHERE id = :id AND user_id = :userId")
        .param("id", id)
        .param("userId", userId)
        .query(Discipline.class)
        .optional();
  }

  public Discipline insert(long userId, DisciplineRequest request) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(
            "INSERT INTO disciplines (user_id, name, color, icon, weight)"
                + " VALUES (:userId, :name, :color, :icon, :weight)")
        .param("userId", userId)
        .param("name", request.name().trim())
        .param("color", request.color())
        .param("icon", request.icon())
        .param("weight", request.weight())
        .update(keys);
    return findById(userId, keys.getKey().longValue()).orElseThrow();
  }

  public boolean update(long userId, long id, DisciplineRequest request) {
    return jdbc.sql(
                "UPDATE disciplines SET name = :name, color = :color, icon = :icon,"
                    + " weight = :weight WHERE id = :id AND user_id = :userId")
            .param("name", request.name().trim())
            .param("color", request.color())
            .param("icon", request.icon())
            .param("weight", request.weight())
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }

  public boolean delete(long userId, long id) {
    return jdbc.sql("DELETE FROM disciplines WHERE id = :id AND user_id = :userId")
            .param("id", id)
            .param("userId", userId)
            .update()
        > 0;
  }
}
