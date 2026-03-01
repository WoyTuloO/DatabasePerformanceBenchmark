package com.example.performanceTesting.domain.model;

import java.util.UUID;

public record Course(
        UUID id,
        String name,
        String department,
        UUID instructorId
) {}