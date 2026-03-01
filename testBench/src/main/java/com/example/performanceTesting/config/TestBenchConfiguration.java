package com.example.performanceTesting.config;

import com.example.performanceTesting.adapters.out.persistence.PostgresStudentAdapter;
import com.example.performanceTesting.adapters.out.persistence.RedisStudentAdapter;
import com.example.performanceTesting.application.service.PostgresService;
import com.example.performanceTesting.application.service.RedisService;
import com.example.performanceTesting.domain.model.Student;
import com.example.performanceTesting.domain.port.PostgresStudentProvider;
import com.example.performanceTesting.domain.port.RedisStudentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class TestBenchConfiguration {

    @Bean
    public RedisService redisPerformanceTestBenchService(RedisStudentProvider redisStudentProvider) {
        return new RedisService(redisStudentProvider);
    }

    @Bean
    public PostgresService postgresPerformanceTestBenchService(PostgresStudentProvider postgresStudentProvider ) {
        return new PostgresService(postgresStudentProvider);
    }

    @Bean
    public RedisTemplate<String, Student> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Student> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisStudentProvider redisStudentProvider(RedisTemplate<String, Student> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        return new RedisStudentAdapter(redisTemplate, stringRedisTemplate);
    }

    @Bean
    PostgresStudentProvider postgresStudentProvider(JdbcTemplate jdbcTemplate) {
        return new PostgresStudentAdapter(jdbcTemplate);
    }
}


