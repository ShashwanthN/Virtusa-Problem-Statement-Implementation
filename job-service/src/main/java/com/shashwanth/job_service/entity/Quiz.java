package com.shashwanth.job_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter
@Setter
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer totalTimeMinutes;
    private Integer numQuestions;
    
    @Column(columnDefinition = "TEXT")
    private String quizDataJson;
}
