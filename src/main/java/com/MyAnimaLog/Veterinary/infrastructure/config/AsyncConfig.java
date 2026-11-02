package com.MyAnimaLog.Veterinary.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async}, used by {@code VerificationAiAnalysisListener} to run IA analysis off-thread. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
