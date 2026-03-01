package com.example.performanceTesting.adapters.in.rest;

import com.example.performanceTesting.application.service.PostgresService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postgres/students")
public class PostgresController extends PerformanceTestControllerTemplate {

    public PostgresController(PostgresService service) {
        super(service);
    }
}
