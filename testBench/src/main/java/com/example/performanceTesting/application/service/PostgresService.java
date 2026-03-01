package com.example.performanceTesting.application.service;

import com.example.performanceTesting.domain.port.PostgresStudentProvider;

public class PostgresService extends TestBenchService{

    public PostgresService(PostgresStudentProvider studentProvider){
        super(studentProvider);
    }
}
