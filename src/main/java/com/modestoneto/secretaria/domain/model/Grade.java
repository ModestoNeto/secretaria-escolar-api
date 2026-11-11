package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;

/** Nota de uma matrícula em uma avaliação. */
public record Grade(Long id, Long assessmentId, Long enrollmentId, BigDecimal score) {
}
