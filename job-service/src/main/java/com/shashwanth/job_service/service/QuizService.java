package com.shashwanth.job_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shashwanth.job_service.entity.Quiz;
import com.shashwanth.job_service.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Long createQuiz(Map<String, Object> request) {
        Map<String, Object> quizRecord = new HashMap<>();
        quizRecord.put("title", request.get("title"));
        quizRecord.put("description", request.get("description"));
        quizRecord.put("questions", request.get("questions"));
        if (request.get("requiredCorrectAnswers") != null) {
            quizRecord.put("requiredCorrectAnswers", request.get("requiredCorrectAnswers"));
        }
        if (request.get("totalTimeMinutes") != null) {
            quizRecord.put("totalTimeMinutes", request.get("totalTimeMinutes"));
        }

        Quiz quizEntity = new Quiz();
        
        if (request.get("totalTimeMinutes") != null) {
            quizEntity.setTotalTimeMinutes(((Number)request.get("totalTimeMinutes")).intValue());
        }
        if (request.get("numQuestions") != null) {
            quizEntity.setNumQuestions(((Number)request.get("numQuestions")).intValue());
        } else if (request.get("questions") instanceof List<?> questions) {
            quizEntity.setNumQuestions(questions.size());
        }

        try {
            quizEntity.setQuizDataJson(objectMapper.writeValueAsString(quizRecord));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        Quiz saved = quizRepository.save(quizEntity);
        return saved.getId();
    }

    private Map<String, Object> getQuizMap(Long quizId) {
        Quiz quizEntity = quizRepository.findById(quizId).orElseThrow();
        try {
            return objectMapper.readValue(quizEntity.getQuizDataJson(), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> startTest(Long quizId) {
        Map<String, Object> quiz = getQuizMap(quizId);
        List<Map<String, Object>> questions = (List<Map<String, Object>>) quiz.get("questions");
        int totalQuestions = questions != null ? questions.size() : 0;
        int requiredCorrectAnswers = resolveRequiredCorrectAnswers(quiz, totalQuestions);
        
        List<Map<String, Object>> questionResponses = new ArrayList<>();
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                Map<String, Object> qr = new HashMap<>();
                qr.put("index", i);
                qr.put("question", q.get("question"));
                qr.put("options", q.get("options"));
                questionResponses.add(qr);
            }
        }

        Object savedTime = quiz.get("totalTimeMinutes");
        int totalTime = savedTime != null ? ((Number)savedTime).intValue() : (questions != null ? questions.size() * 2 : 0);

        Map<String, Object> response = new HashMap<>();
        response.put("quizId", quizId);
        response.put("totalTimeMinutes", totalTime);
        response.put("questions", questionResponses);
        response.put("requiredCorrectAnswers", requiredCorrectAnswers);
        return response;
    }

    public Map<String, Object> submitTest(Long quizId, Map<String, Object> request) {
        Map<String, Object> quiz = getQuizMap(quizId);
        List<Map<String, Object>> questions = (List<Map<String, Object>>) quiz.get("questions");
        List<Integer> answers = (List<Integer>) request.get("answers");
        int total = questions != null ? questions.size() : 0;
        int requiredCorrectAnswers = resolveRequiredCorrectAnswers(quiz, total);

        int correct = 0;
        if (questions != null && answers != null) {
            for (int i = 0; i < questions.size(); i++) {
                if (i >= answers.size()) {
                    continue;
                }
                Integer expected = (Integer) questions.get(i).get("correctOptionIndex");
                if (expected != null && expected.equals(answers.get(i))) {
                    correct++;
                }
            }
        }

        double scorePercent = total == 0 ? 0.0 : (correct * 100.0) / total;
        boolean passed = total > 0 && correct >= requiredCorrectAnswers;
        
        Map<String, Object> response = new HashMap<>();
        response.put("quizId", quizId);
        response.put("totalQuestions", total);
        response.put("correctAnswers", correct);
        response.put("scorePercent", scorePercent);
        response.put("requiredCorrectAnswers", requiredCorrectAnswers);
        response.put("passed", passed);
        response.put("summary", "You answered " + correct + " out of " + total + " correctly. Required to pass: " + requiredCorrectAnswers + ".");
        return response;
    }

    private int resolveRequiredCorrectAnswers(Map<String, Object> quiz, int totalQuestions) {
        if (totalQuestions <= 0) {
            return 0;
        }

        Object configuredValue = quiz.get("requiredCorrectAnswers");
        if (configuredValue instanceof Number numberValue) {
            int requested = numberValue.intValue();
            if (requested < 1) {
                return 1;
            }
            return Math.min(requested, totalQuestions);
        }

        return Math.max(1, (int) Math.ceil(totalQuestions * 0.6));
    }
}
