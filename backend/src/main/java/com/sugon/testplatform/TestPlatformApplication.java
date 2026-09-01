package com.sugon.testplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.sugon.testplatform.mapper")
public class TestPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestPlatformApplication.class, args);
    }
}
