package com.modestoneto.secretaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Injetável para os testes fixarem a data.
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
