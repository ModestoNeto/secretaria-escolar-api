package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Frequência de uma matrícula em uma disciplina: aulas registradas e presenças. */
public record AttendanceSummary(Long classSubjectId, Long subjectId, String subjectName, long lessons, long presences) {

    public long absences() {
        return lessons - presences;
    }

    /** Percentual de presença com 1 casa; sem aulas registradas é 100 (ninguém faltou a nada ainda). */
    public BigDecimal percentage() {
        if (lessons == 0) {
            return new BigDecimal("100.0");
        }
        return BigDecimal.valueOf(presences * 100).divide(BigDecimal.valueOf(lessons), 1, RoundingMode.HALF_UP);
    }
}
