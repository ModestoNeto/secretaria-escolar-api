package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;

import java.time.Instant;

/** Linha da lista de matrículas de uma turma: a matrícula e o aluno que a tem. */
record ClassEnrollmentResponse(Long id, Long studentId, String studentName, String registrationNumber,
                               EnrollmentStatus status, Instant enrolledAt) {

    static ClassEnrollmentResponse from(EnrolledStudent e) {
        var m = e.enrollment();
        return new ClassEnrollmentResponse(m.id(), m.studentId(), e.studentName(), e.registrationNumber(),
                m.status(), m.enrolledAt());
    }
}
