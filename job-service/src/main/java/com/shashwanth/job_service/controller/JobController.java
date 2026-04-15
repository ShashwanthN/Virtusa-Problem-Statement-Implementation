package com.shashwanth.job_service.controller;

import com.shashwanth.job_service.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;

    @GetMapping
    public List<Map<String, Object>> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PostMapping("/generate-quiz")
    public List<Map<String, Object>> generateQuizProxy(@RequestBody Map<String, Object> request) {
        return jobService.generateQuizProxy(request);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> apply(@RequestPart("file") MultipartFile file) {
        return jobService.applyToJob(file);
    }

    @PostMapping(value = "/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> applyToSpecificJob(
            @PathVariable Long jobId,
            @RequestPart("file") MultipartFile file
    ) {
        return jobService.applyToSpecificJob(jobId, file);
    }

    @PostMapping(value = "/{jobId}/match-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> analyzeMatchedJob(
            @PathVariable Long jobId,
            @RequestPart("file") MultipartFile file
    ) {
        return jobService.analyzeMatchedJob(jobId, file);
    }

    @PostMapping("/{jobId}/start-test")
    public Map<String, Object> startTest(@PathVariable Long jobId) {
        return jobService.startJobTest(jobId);
    }

    @PostMapping("/{jobId}/submit-test")
    public Map<String, Object> submitTest(
            @PathVariable Long jobId,
            @RequestBody Map<String, Object> request
    ) {
        return jobService.submitJobTest(jobId, request);
    }
}
