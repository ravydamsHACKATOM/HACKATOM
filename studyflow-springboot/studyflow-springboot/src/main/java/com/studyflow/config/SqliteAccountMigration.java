package com.studyflow.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Configuration
public class SqliteAccountMigration {
    private static final List<String> OWNED_TABLES = List.of(
            "disciplines", "study_events", "availabilities", "study_blocks");

    @Bean
    public ApplicationRunner migrateAccountOwnership(JdbcTemplate jdbc) {
        return args -> {
            for (String table : OWNED_TABLES) {
                List<Map<String, Object>> columns = jdbc.queryForList("PRAGMA table_info(" + table + ")");
                boolean hasOwner = columns.stream()
                        .anyMatch(column -> "user_id".equals(column.get("name")));
                if (!hasOwner) {
                    jdbc.execute("ALTER TABLE " + table
                            + " ADD COLUMN user_id INTEGER REFERENCES users(id)");
                }
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_" + table
                        + "_user_id ON " + table + "(user_id)");
            }
        };
    }
}
