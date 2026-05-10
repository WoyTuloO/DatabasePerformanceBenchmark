package com.example.performanceTesting.bootstrap;

import com.example.performanceTesting.bootstrap.config.DatabaseType;

public interface DatabaseSeeder {

    DatabaseType type();

    void bootstrap();
}

