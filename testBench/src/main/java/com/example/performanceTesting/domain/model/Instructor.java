package com.example.performanceTesting.domain.model;

import java.util.UUID;

public record Instructor(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String department
) {}