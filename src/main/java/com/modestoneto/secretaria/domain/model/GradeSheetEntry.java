package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;

/** Linha da pauta: um aluno matriculado e a nota dele na avaliação (nula se ainda não lançada). */
public record GradeSheetEntry(Long enrollmentId, Long studentId, String studentName, String registrationNumber,
                              BigDecimal score) {
}
