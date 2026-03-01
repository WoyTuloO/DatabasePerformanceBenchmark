package com.example.performanceTesting.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record Student(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String department,
        double gpa,
        LocalDateTime createdAt
) implements Serializable {
}
