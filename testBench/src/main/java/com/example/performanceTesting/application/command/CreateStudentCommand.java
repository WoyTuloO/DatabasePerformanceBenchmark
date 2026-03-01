package com.example.performanceTesting.application.command;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateStudentCommand(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String department,
        double gpa,
        LocalDateTime createdAt
) {
}
