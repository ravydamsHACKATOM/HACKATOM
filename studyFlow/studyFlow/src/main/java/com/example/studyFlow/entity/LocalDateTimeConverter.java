package com.example.studyFlow.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {
    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > 10 && normalized.charAt(10) == ' ') {
            normalized = normalized.substring(0, 10) + 'T' + normalized.substring(11);
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
