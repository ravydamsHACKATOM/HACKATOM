package com.example.studyFlow.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DatabaseConfiguration {
    @Bean
    static BeanFactoryPostProcessor createDatabaseDirectory(Environment environment) throws IOException {
        String databasePath = environment.getProperty(
                "STUDYFLOW_DB_PATH",
                Path.of(System.getProperty("user.home"), ".studyflow", "study-flow.db").toString());
        Path parent = Path.of(databasePath).toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        return beanFactory -> { };
    }
}
