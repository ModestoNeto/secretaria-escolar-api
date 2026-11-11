package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.AttendanceSheetEntry;

/** present é nulo enquanto a chamada do aluno naquela aula não foi registrada. */
record AttendanceSheetResponse(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                               Boolean present) {

    static AttendanceSheetResponse from(AttendanceSheetEntry e) {
        return new AttendanceSheetResponse(e.enrollmentId(), e.studentId(), e.studentName(), e.registrationNumber(), e.present());
    }
}
