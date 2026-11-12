package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;

/** Nota de uma matrícula em uma avaliação, com o peso e o máximo dela, para o cálculo da média. */
public record GradeLine(Long classSubjectId, int term, BigDecimal weight, BigDecimal maxScore, BigDecimal score) {
}
