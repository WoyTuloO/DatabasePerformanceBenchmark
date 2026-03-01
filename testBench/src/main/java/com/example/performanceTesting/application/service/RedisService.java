package com.example.performanceTesting.application.service;

import com.example.performanceTesting.domain.port.RedisStudentProvider;

public class RedisService extends TestBenchService {

    public RedisService(RedisStudentProvider studentProvider){
        super(studentProvider);
    }
}
