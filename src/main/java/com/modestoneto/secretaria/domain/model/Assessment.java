package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;

/** Avaliação de uma disciplina em uma turma (classSubjectId é o vínculo). term é o bimestre, de 1 a 4. */
public record Assessment(Long id, Long classSubjectId, String name, int term, BigDecimal weight, BigDecimal maxScore) {
}
