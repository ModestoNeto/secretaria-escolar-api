package com.modestoneto.secretaria.domain.model;

import java.time.Instant;

/** Matrícula de um aluno em uma turma. schoolYear é o ano da turma no momento da matrícula. */
public record Enrollment(Long id, Long studentId, Long classId, int schoolYear, EnrollmentStatus status,
                         Instant enrolledAt) {

    public boolean active() {
        return status == EnrollmentStatus.ACTIVE;
    }

    public Enrollment cancelled() {
        return new Enrollment(id, studentId, classId, schoolYear, EnrollmentStatus.CANCELLED, enrolledAt);
    }
}
