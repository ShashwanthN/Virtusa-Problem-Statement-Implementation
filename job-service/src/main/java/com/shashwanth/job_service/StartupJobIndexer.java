package com.shashwanth.job_service;

import com.shashwanth.job_service.repository.JobRepository;
import com.shashwanth.job_service.service.PythonBackendService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StartupJobIndexer {

    private final JobRepository jobRepository;
    private final PythonBackendService pythonBackendService;

    @Bean
    ApplicationRunner syncSavedJobsToPythonIndexer() {
        return args -> pythonBackendService.syncMissingJobsOnStartup(jobRepository.findAll());
    }
}
