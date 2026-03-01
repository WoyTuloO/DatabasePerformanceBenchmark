package com.example.performanceTesting.adapters.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetStudentsByDepartmentResponse(
        List<StudentResponse> students
) {
    public record StudentResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String department,
            double gpa,
            LocalDateTime createdAt
    ) {}
}
