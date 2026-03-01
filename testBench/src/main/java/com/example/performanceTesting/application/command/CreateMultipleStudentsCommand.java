package com.example.performanceTesting.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateMultipleStudentsCommand(
        List<Student> studentsToCreate
) {
    public record Student(UUID id,
                          String firstName,
                          String lastName,
                          String email,
                          String department,
                          double gpa,
                          LocalDateTime createdAt
    ){}
}
