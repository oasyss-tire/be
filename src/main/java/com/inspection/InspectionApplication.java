package com.inspection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = "com.inspection",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.inspection\\.nice\\..*"
    )
)
@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
public class InspectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspectionApplication.class, args);
    }
}