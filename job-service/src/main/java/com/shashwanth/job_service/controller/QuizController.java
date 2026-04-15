package com.shashwanth.job_service.controller;

import com.shashwanth.job_service.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public Map<String, Object> createQuiz(@RequestBody Map<String, Object> request) {
        return Map.of("quizId", quizService.createQuiz(request));
    }

    @GetMapping("/{quizId}/start-test")
    public Map<String, Object> startTest(@PathVariable Long quizId) {
        return quizService.startTest(quizId);
    }

    @PostMapping("/{quizId}/submit-test")
    public Map<String, Object> submitTest(
            @PathVariable Long quizId,
            @RequestBody Map<String, Object> request
    ) {
        return quizService.submitTest(quizId, request);
    }
}
