package com.example.performanceTesting.adapters.in.rest;

import com.example.performanceTesting.application.service.RedisService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis/students")
public class RedisController extends PerformanceTestControllerTemplate{

    public RedisController(RedisService service) {
        super(service);
    }
}
