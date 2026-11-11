package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

// Só ids, como nas outras entidades (sem @ManyToOne).
@Entity
@Table(name = "attendance_records")
class AttendanceRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_subject_id", nullable = false)
    private Long classSubjectId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(nullable = false)
    private boolean present;

    protected AttendanceRecordJpaEntity() {
    }

    AttendanceRecordJpaEntity(Long id, Long classSubjectId, LocalDate lessonDate, Long enrollmentId, boolean present) {
        this.id = id;
        this.classSubjectId = classSubjectId;
        this.lessonDate = lessonDate;
        this.enrollmentId = enrollmentId;
        this.present = present;
    }

    Long getId() { return id; }
    Long getClassSubjectId() { return classSubjectId; }
    LocalDate getLessonDate() { return lessonDate; }
    Long getEnrollmentId() { return enrollmentId; }
    boolean isPresent() { return present; }
}
