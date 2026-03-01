package com.example.performanceTesting.adapters.dto.request;

import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateMultipleStudentsRequest(
        List<CreateMultipleStudentsCommand.Student> studentsToCreate
) {
    public record Student(UUID id,
                          String firstName,
                          String lastName,
                          String email,
                          String department,
                          double gpa,
                          LocalDateTime createdAt
    ) {
    }
}
