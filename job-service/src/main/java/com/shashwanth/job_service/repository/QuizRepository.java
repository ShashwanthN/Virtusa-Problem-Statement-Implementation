package com.shashwanth.job_service.repository;

import com.shashwanth.job_service.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}
