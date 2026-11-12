package com.modestoneto.secretaria.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Regras do boletim. Cada nota é levada para a escala 0 a 10 (nota / máximo * 10) e a média é ponderada pelo peso.
 * APROVADO: média >= 6,0 e frequência >= 75%; RECUPERAÇÃO: média < 6,0 com frequência suficiente;
 * REPROVADO: frequência < 75% (mesmo com média alta).
 */
public final class ReportCardCalculator {

    static final BigDecimal MIN_AVERAGE = new BigDecimal("6.0");
    static final int MIN_ATTENDANCE_PERCENT = 75;
    private static final BigDecimal TEN = BigDecimal.TEN;

    private ReportCardCalculator() {
    }

    public static SubjectReport subject(SubjectRef ref, List<GradeLine> lines, long lessons, long presences) {
        Map<Integer, List<GradeLine>> byTerm = lines.stream()
                .collect(Collectors.groupingBy(GradeLine::term, TreeMap::new, Collectors.toList()));
        List<TermAverage> terms = byTerm.entrySet().stream()
                .map(e -> new TermAverage(e.getKey(), average(e.getValue()))).toList();
        BigDecimal average = lines.isEmpty() ? null : average(lines);

        AttendanceSummary attendance = new AttendanceSummary(ref.classSubjectId(), ref.subjectId(), ref.subjectName(), lessons, presences);
        return new SubjectReport(ref.subjectId(), ref.subjectName(), terms, average, lessons, presences,
                attendance.percentage(), resultOf(average, lessons, presences));
    }

    public static ReportResult overall(List<SubjectReport> subjects) {
        if (subjects.stream().anyMatch(s -> s.result() == ReportResult.FAILED)) {
            return ReportResult.FAILED;
        }
        if (subjects.stream().anyMatch(s -> s.result() == ReportResult.RECOVERY)) {
            return ReportResult.RECOVERY;
        }
        if (subjects.isEmpty() || subjects.stream().anyMatch(s -> s.result() == ReportResult.IN_PROGRESS)) {
            return ReportResult.IN_PROGRESS;
        }
        return ReportResult.APPROVED;
    }

    private static ReportResult resultOf(BigDecimal average, long lessons, long presences) {
        // conta exata (sem arredondar o percentual): 74,96% não vira 75%
        if (lessons > 0 && presences * 100 < MIN_ATTENDANCE_PERCENT * lessons) {
            return ReportResult.FAILED;
        }
        if (average == null) {
            return ReportResult.IN_PROGRESS;
        }
        return average.compareTo(MIN_AVERAGE) >= 0 ? ReportResult.APPROVED : ReportResult.RECOVERY;
    }

    private static BigDecimal average(List<GradeLine> lines) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (GradeLine l : lines) {
            BigDecimal normalized = l.score().multiply(TEN).divide(l.maxScore(), 6, RoundingMode.HALF_UP);
            weighted = weighted.add(normalized.multiply(l.weight()));
            weights = weights.add(l.weight());
        }
        return weighted.divide(weights, 2, RoundingMode.HALF_UP);
    }
}
