package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Só ids, como no vínculo (sem @ManyToOne). A coluna active_year é gerada pelo banco e fica fora da entidade.
@Entity
@Table(name = "enrollments")
class EnrollmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "school_year", nullable = false)
    private int schoolYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    protected EnrollmentJpaEntity() {
    }

    EnrollmentJpaEntity(Long id, Long studentId, Long classId, int schoolYear, EnrollmentStatus status, Instant enrolledAt) {
        this.id = id;
        this.studentId = studentId;
        this.classId = classId;
        this.schoolYear = schoolYear;
        this.status = status;
        this.enrolledAt = enrolledAt;
    }

    Long getId() { return id; }
    Long getStudentId() { return studentId; }
    Long getClassId() { return classId; }
    int getSchoolYear() { return schoolYear; }
    EnrollmentStatus getStatus() { return status; }
    Instant getEnrolledAt() { return enrolledAt; }
}
