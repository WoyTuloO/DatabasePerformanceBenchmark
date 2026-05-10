package com.example.performanceTesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(excludeName = {
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetailsAutoConfiguration"
})
@ConfigurationPropertiesScan
public class PerformanceTestingApplication {
	public static void main(String[] args) {
		SpringApplication.run(PerformanceTestingApplication.class, args);
	}
}
