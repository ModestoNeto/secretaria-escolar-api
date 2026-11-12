package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** average é nula sem nota lançada; attendance é o percentual de presença (100,0 sem aulas registradas). */
public record SubjectReport(Long subjectId, String subjectName, List<TermAverage> terms, BigDecimal average,
                            long lessons, long presences, BigDecimal attendance, ReportResult result) {
}
