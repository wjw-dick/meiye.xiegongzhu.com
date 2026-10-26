package com.meiye;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@MapperScan("com.meiye.domain.mapper")
@SpringBootApplication
public class MeiyeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeiyeApplication.class, args);
    }
}
