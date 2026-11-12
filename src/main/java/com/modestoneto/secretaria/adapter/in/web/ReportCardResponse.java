package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.ReportCard;
import com.modestoneto.secretaria.domain.model.ReportResult;
import com.modestoneto.secretaria.domain.model.SubjectReport;

import java.math.BigDecimal;
import java.util.List;

/** average é nula sem nota lançada; terms traz só os bimestres que têm nota. */
record ReportCardResponse(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                          Long classId, String className, int schoolYear, ReportResult result, List<Subject> subjects) {

    record Term(int term, BigDecimal average) {
    }

    record Subject(Long subjectId, String subjectName, List<Term> terms, BigDecimal average, long lessons,
                   long presences, BigDecimal attendance, ReportResult result) {

        static Subject from(SubjectReport s) {
            return new Subject(s.subjectId(), s.subjectName(),
                    s.terms().stream().map(t -> new Term(t.term(), t.average())).toList(),
                    s.average(), s.lessons(), s.presences(), s.attendance(), s.result());
        }
    }

    static ReportCardResponse from(ReportCard r) {
        return new ReportCardResponse(r.enrollmentId(), r.studentId(), r.studentName(), r.registrationNumber(),
                r.classId(), r.className(), r.schoolYear(), r.result(), r.subjects().stream().map(Subject::from).toList());
    }
}
