package com.modestoneto.secretaria.domain.model;

import java.util.List;

public record ReportCard(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                         Long classId, String className, int schoolYear, List<SubjectReport> subjects,
                         ReportResult result) {
}
