package com.modestoneto.secretaria.domain.model;

import java.time.LocalDate;

/** Presença (ou falta) de uma matrícula em uma aula da disciplina da turma. */
public record AttendanceRecord(Long id, Long classSubjectId, LocalDate lessonDate, Long enrollmentId, boolean present) {
}
