package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;

import java.time.Instant;

record EnrollmentResponse(Long id, Long studentId, Long classId, int schoolYear, EnrollmentStatus status,
                          Instant enrolledAt) {

    static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(e.id(), e.studentId(), e.classId(), e.schoolYear(), e.status(), e.enrolledAt());
    }
}
