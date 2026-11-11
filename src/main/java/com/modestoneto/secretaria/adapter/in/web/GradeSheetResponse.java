package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.GradeSheetEntry;

import java.math.BigDecimal;

/** score é nulo enquanto a nota do aluno não foi lançada. */
record GradeSheetResponse(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                          BigDecimal score) {

    static GradeSheetResponse from(GradeSheetEntry e) {
        return new GradeSheetResponse(e.enrollmentId(), e.studentId(), e.studentName(), e.registrationNumber(), e.score());
    }
}
