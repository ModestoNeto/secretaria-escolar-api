package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "grades")
class GradeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    protected GradeJpaEntity() {
    }

    GradeJpaEntity(Long id, Long assessmentId, Long enrollmentId, BigDecimal score) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.enrollmentId = enrollmentId;
        this.score = score;
    }

    Long getId() { return id; }
    Long getAssessmentId() { return assessmentId; }
    Long getEnrollmentId() { return enrollmentId; }
    BigDecimal getScore() { return score; }
}
