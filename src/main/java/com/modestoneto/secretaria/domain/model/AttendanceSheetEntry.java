package com.modestoneto.secretaria.domain.model;

/** Linha da chamada: um aluno matriculado e a presença dele na aula (nula se a chamada ainda não foi feita). */
public record AttendanceSheetEntry(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                                   Boolean present) {
}
