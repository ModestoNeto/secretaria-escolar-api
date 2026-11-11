package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.AttendanceSummary;

import java.math.BigDecimal;

record AttendanceSummaryResponse(Long subjectId, String subjectName, long lessons, long presences, long absences,
                                 BigDecimal percentage) {

    static AttendanceSummaryResponse from(AttendanceSummary s) {
        return new AttendanceSummaryResponse(s.subjectId(), s.subjectName(), s.lessons(), s.presences(), s.absences(),
                s.percentage());
    }
}
