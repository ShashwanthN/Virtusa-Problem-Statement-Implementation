package com.shashwanth.job_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String companyId;

    private String companyName;

    private String skillsNeeded;

    private Integer minExperienceYears;

    private String employmentType;

    private String location;

    private Double minSalary;

    private Double maxSalary;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private Long quizId;
}