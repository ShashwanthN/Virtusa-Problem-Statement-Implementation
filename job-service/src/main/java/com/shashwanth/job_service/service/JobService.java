package com.shashwanth.job_service.service;

import com.shashwanth.job_service.entity.Job;
import com.shashwanth.job_service.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final PythonBackendService pythonBackendService;
    private final QuizService quizService;

    public List<Map<String, Object>> generateQuizProxy(Map<String, Object> request) {
        return pythonBackendService.generateQuiz(request);
    }

    public Map<String, Object> createJob(Map<String, Object> request) {
        Job job = new Job();
        job.setTitle((String) request.get("title"));
        job.setDescription((String) request.get("description"));
        job.setCompanyId((String) request.get("companyId"));
        job.setCompanyName((String) request.get("companyName"));

        job.setSkillsNeeded((String) request.get("skillsNeeded")); 
        
        job.setMinExperienceYears(((Number) request.get("minExperienceYears")).intValue());
        job.setEmploymentType((String) request.get("employmentType"));
        job.setLocation((String) request.get("location"));
        job.setMinSalary(((Number) request.get("minSalary")).doubleValue());
        job.setMaxSalary(((Number) request.get("maxSalary")).doubleValue());

        job.setQuizId(quizService.createQuiz(request));

        Job saved = jobRepository.save(job);
        pythonBackendService.indexJob(saved);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("title", saved.getTitle());
        response.put("quizId", saved.getQuizId());
        return response;
    }

    public Map<String, Object> applyToJob(MultipartFile resumeFile) {
        Map<String, Object> matchResponse = pythonBackendService.matchResume(resumeFile);

        List<Number> ids = (List<Number>) matchResponse.get("jobIds");
        if (ids == null || ids.isEmpty()) {
            return Map.of("matchedJobIds", List.of(), "results", List.of());
        }
        List<Long> matchedJobIds = ids.stream().map(Number::longValue).collect(Collectors.toList());

        Map<Long, Job> jobMap = new HashMap<>();
        jobRepository.findAllById(matchedJobIds).forEach(job -> jobMap.put(job.getId(), job));

        List<Map<String, Object>> matches = (List<Map<String, Object>>) matchResponse.get("matches");
        Map<Long, Map<String, Object>> detailsByJobId = new HashMap<>();
        matches.forEach(m -> detailsByJobId.put(((Number) m.get("jobId")).longValue(), m));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Long jobId : matchedJobIds) {
            Job job = jobMap.get(jobId);
            Map<String, Object> details = detailsByJobId.get(jobId);
            if (job == null || details == null) {
                continue;
            }

            Map<String, Object> jobRes = new HashMap<>();
            jobRes.put("id", job.getId());
            jobRes.put("title", job.getTitle());
            jobRes.put("quizId", job.getQuizId());
            jobRes.put("companyName", job.getCompanyName());
            jobRes.put("description", job.getDescription());
            jobRes.put("skillsNeeded", job.getSkillsNeeded());
            jobRes.put("employmentType", job.getEmploymentType());
            jobRes.put("location", job.getLocation());
            jobRes.put("minExperienceYears", job.getMinExperienceYears());

            Map<String, Object> result = new HashMap<>();
            result.put("job", jobRes);
            result.put("matchScore", details.get("matchScore"));
            result.put("matchedSkills", details.get("matchedSkills"));
            result.put("missingSkills", details.get("missingSkills"));
            result.put("analysisSummary", details.get("analysisSummary"));
            result.put("recommendation", details.get("recommendation"));
            results.add(result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("matchedJobIds", matchedJobIds);
        response.put("results", results);
        return response;
    }

    public Map<String, Object> applyToSpecificJob(Long jobId, MultipartFile resumeFile) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        Map<String, Object> matchResponse = pythonBackendService.matchResume(resumeFile);

        List<Map<String, Object>> matches = (List<Map<String, Object>>) matchResponse.get("matches");
        Map<String, Object> details = null;
        for (Map<String, Object> match : matches) {
            if (((Number) match.get("jobId")).longValue() == jobId) {
                details = match;
                break;
            }
        }

        Map<String, Object> jobRes = new HashMap<>();
        jobRes.put("id", job.getId());
        jobRes.put("title", job.getTitle());
        jobRes.put("quizId", job.getQuizId());
        jobRes.put("companyName", job.getCompanyName());
        jobRes.put("description", job.getDescription());
        jobRes.put("skillsNeeded", job.getSkillsNeeded());
        jobRes.put("employmentType", job.getEmploymentType());
        jobRes.put("location", job.getLocation());
        jobRes.put("minExperienceYears", job.getMinExperienceYears());

        Map<String, Object> response = new HashMap<>();
        response.put("job", jobRes);
        response.put("matchScore", details != null ? details.get("matchScore") : 0);
        response.put("matchedSkills", details != null ? details.get("matchedSkills") : List.of());
        response.put("missingSkills", details != null ? details.get("missingSkills") : List.of());
        response.put("analysisSummary", details != null ? details.get("analysisSummary") : "No analysis available.");
        response.put("recommendation", details != null ? details.get("recommendation") : "No recommendation available.");
        return response;
    }

    public Map<String, Object> analyzeMatchedJob(Long jobId, MultipartFile resumeFile) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        Map<String, Object> analysis = pythonBackendService.analyzeMatch(resumeFile, job);

        Map<String, Object> jobRes = new HashMap<>();
        jobRes.put("id", job.getId());
        jobRes.put("title", job.getTitle());
        jobRes.put("quizId", job.getQuizId());
        jobRes.put("companyName", job.getCompanyName());
        jobRes.put("description", job.getDescription());
        jobRes.put("skillsNeeded", job.getSkillsNeeded());
        jobRes.put("employmentType", job.getEmploymentType());
        jobRes.put("location", job.getLocation());
        jobRes.put("minExperienceYears", job.getMinExperienceYears());

        Map<String, Object> response = new HashMap<>();
        response.put("job", jobRes);
        response.put("matchScore", analysis.get("matchScore"));
        response.put("matchedSkills", analysis.get("matchedSkills"));
        response.put("missingSkills", analysis.get("missingSkills"));
        response.put("analysisSummary", analysis.get("analysisSummary"));
        response.put("recommendation", analysis.get("recommendation"));
        response.put("llmSuggestion", analysis.get("llmSuggestion"));
        return response;
    }

    public List<Map<String, Object>> getAllJobs() {
        List<Map<String, Object>> responses = new ArrayList<>();
        jobRepository.findAll().forEach(job -> {
            Map<String, Object> jobRes = new HashMap<>();
            jobRes.put("id", job.getId());
            jobRes.put("title", job.getTitle());
            jobRes.put("quizId", job.getQuizId());
            jobRes.put("companyName", job.getCompanyName());
            jobRes.put("skillsNeeded", job.getSkillsNeeded());
            jobRes.put("location", job.getLocation());
            jobRes.put("employmentType", job.getEmploymentType());
            responses.add(jobRes);
        });
        return responses;
    }

    public Map<String, Object> startJobTest(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        Map<String, Object> quizResponse = quizService.startTest(job.getQuizId());

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("quizId", job.getQuizId());
        response.put("totalTimeMinutes", quizResponse.get("totalTimeMinutes"));
        response.put("questions", quizResponse.get("questions"));
        response.put("requiredCorrectAnswers", quizResponse.get("requiredCorrectAnswers"));
        return response;
    }

    public Map<String, Object> submitJobTest(Long jobId, Map<String, Object> request) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        Map<String, Object> quizResponse = quizService.submitTest(job.getQuizId(), request);

        Boolean passed = (Boolean) quizResponse.get("passed");

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("quizId", job.getQuizId());
        response.put("totalQuestions", quizResponse.get("totalQuestions"));
        response.put("correctAnswers", quizResponse.get("correctAnswers"));
        response.put("scorePercent", quizResponse.get("scorePercent"));
        response.put("requiredCorrectAnswers", quizResponse.get("requiredCorrectAnswers"));
        response.put("passed", passed);
        response.put("summary", quizResponse.get("summary"));
        response.put("appliedStatus", passed != null && passed);
        return response;
    }
}
