package com.nekoflow.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita @Scheduled para o worker automatico (B2). */
@Configuration
@EnableScheduling
public class WorkerSchedulingConfig {
}
