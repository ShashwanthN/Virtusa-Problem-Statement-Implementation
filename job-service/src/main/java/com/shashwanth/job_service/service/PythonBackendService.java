package com.shashwanth.job_service.service;

import com.shashwanth.job_service.entity.Job;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PythonBackendService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8000";

    public void indexJob(Job job) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.getId());
        payload.put("title", job.getTitle());
        payload.put("description", job.getDescription());
        payload.put("skillsNeeded", job.getSkillsNeeded());
        restTemplate.postForObject(baseUrl + "/api/jobs/index", payload, Void.class);
    }

    public Set<Long> getIndexedJobIds() {
        Map<String, Object> response = restTemplate.getForObject(baseUrl + "/api/jobs/indexed-ids", Map.class);
        Set<Long> indexedIds = new HashSet<>();

        if (response == null) {
            return indexedIds;
        }

        List<Number> rawIds = (List<Number>) response.get("jobIds");
        if (rawIds == null) {
            return indexedIds;
        }

        for (Number rawId : rawIds) {
            indexedIds.add(rawId.longValue());
        }

        return indexedIds;
    }

    public void syncMissingJobs(Collection<Job> jobs) {
        Set<Long> indexedIds = getIndexedJobIds();

        for (Job job : jobs) {
            if (job.getId() == null || indexedIds.contains(job.getId())) {
                continue;
            }
            indexJob(job);
        }
    }

    public void syncMissingJobsOnStartup(Collection<Job> jobs) {
        try {
            syncMissingJobs(jobs);
        } catch (RestClientException exception) {
            System.err.println("Python AI service was unavailable during startup sync: " + exception.getMessage());
        }
    }

    public Map<String, Object> matchResume(MultipartFile resume) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resume.getResource());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(baseUrl + "/api/match", requestEntity, Map.class);
    }

    public Map<String, Object> analyzeMatch(MultipartFile resume, Job job) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resume.getResource());
        body.add("job_title", job.getTitle());
        body.add("job_description", job.getDescription());
        body.add("skills_needed", job.getSkillsNeeded());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(baseUrl + "/api/match/analyze", requestEntity, Map.class);
    }

    public List<Map<String, Object>> generateQuiz(Map<String, Object> request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.get("title"));
        payload.put("description", request.get("description"));
        payload.put("skillsNeeded", request.get("skillsNeeded"));
        payload.put("minExperienceYears", request.get("minExperienceYears"));
        payload.put("numQuestions", request.get("numQuestions"));

        Map<String, Object> response = restTemplate.postForObject(baseUrl + "/api/quiz/generate", payload, Map.class);
        List<Map<String, Object>> questions = new ArrayList<>();

        List<Map<String, Object>> rawQuestions = (List<Map<String, Object>>) response.get("questions");
        for (Map<String, Object> raw : rawQuestions) {
            Map<String, Object> q = new HashMap<>();
            q.put("question", raw.get("question"));
            q.put("options", raw.get("options"));
            q.put("correctOptionIndex", raw.get("correctOptionIndex"));
            questions.add(q);
        }

        return questions;
    }
}
