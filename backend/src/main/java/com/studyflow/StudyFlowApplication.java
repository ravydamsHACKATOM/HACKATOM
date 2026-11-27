package com.studyflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudyFlowApplication {

  public static void main(String[] args) throws IOException {
    // SQLite creates the database file but not its parent folder.
    Files.createDirectories(Path.of("data"));
    SpringApplication.run(StudyFlowApplication.class, args);
  }
}
