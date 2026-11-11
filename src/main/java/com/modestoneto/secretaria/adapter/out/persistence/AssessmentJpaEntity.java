package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

// Só ids, como nas outras entidades (sem @ManyToOne).
@Entity
@Table(name = "assessments")
class AssessmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_subject_id", nullable = false)
    private Long classSubjectId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private int term;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    protected AssessmentJpaEntity() {
    }

    AssessmentJpaEntity(Long id, Long classSubjectId, String name, int term, BigDecimal weight, BigDecimal maxScore) {
        this.id = id;
        this.classSubjectId = classSubjectId;
        this.name = name;
        this.term = term;
        this.weight = weight;
        this.maxScore = maxScore;
    }

    Long getId() { return id; }
    Long getClassSubjectId() { return classSubjectId; }
    String getName() { return name; }
    int getTerm() { return term; }
    BigDecimal getWeight() { return weight; }
    BigDecimal getMaxScore() { return maxScore; }
}
