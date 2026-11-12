package com.modestoneto.secretaria.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCardCalculatorTest {

    private static final SubjectRef MAT = new SubjectRef(1L, 10L, "Matemática");

    private static GradeLine nota(int bimestre, String peso, String maximo, String nota) {
        return new GradeLine(1L, bimestre, new BigDecimal(peso), new BigDecimal(maximo), new BigDecimal(nota));
    }

    private static SubjectReport calcular(List<GradeLine> notas, long aulas, long presencas) {
        return ReportCardCalculator.subject(MAT, notas, aulas, presencas);
    }

    @Test
    void mediaPonderadaNormalizaCadaNotaParaEscalaDeDez() {
        // 8 de 10 (peso 2) vale 8,0 e 5 de 5 (peso 1) vale 10,0: (8*2 + 10*1) / 3 = 8,67
        SubjectReport r = calcular(List.of(nota(1, "2", "10", "8"), nota(1, "1", "5", "5")), 0, 0);

        assertThat(r.average()).isEqualByComparingTo("8.67");
    }

    @Test
    void mediaPorBimestreSoTrazOsBimestresComNota() {
        SubjectReport r = calcular(List.of(nota(1, "1", "10", "4"), nota(1, "1", "10", "6"), nota(3, "1", "10", "9")), 0, 0);

        assertThat(r.terms()).extracting(TermAverage::term).containsExactly(1, 3);
        assertThat(r.terms().get(0).average()).isEqualByComparingTo("5.00");
        assertThat(r.terms().get(1).average()).isEqualByComparingTo("9.00");
        assertThat(r.average()).isEqualByComparingTo("6.33");
    }

    @Test
    void mediaSeisComFrequenciaSuficienteAprova() {
        assertThat(calcular(List.of(nota(1, "1", "10", "6")), 4, 3).result()).isEqualTo(ReportResult.APPROVED);
    }

    @Test
    void mediaAbaixoDeSeisVaiParaRecuperacao() {
        assertThat(calcular(List.of(nota(1, "1", "10", "5.99")), 4, 4).result()).isEqualTo(ReportResult.RECOVERY);
    }

    @Test
    void frequenciaAbaixoDeSetentaECincoReprovaMesmoComMediaAlta() {
        SubjectReport r = calcular(List.of(nota(1, "1", "10", "10")), 100, 74);

        assertThat(r.result()).isEqualTo(ReportResult.FAILED);
        assertThat(r.attendance()).isEqualByComparingTo("74.0");
    }

    @Test
    void frequenciaExatamenteSetentaECincoNaoReprova() {
        assertThat(calcular(List.of(nota(1, "1", "10", "7")), 4, 3).result()).isEqualTo(ReportResult.APPROVED);
    }

    @Test
    void setentaECincoMenosUmaFracaoNaoArredondaParaCima() {
        // 749 de 1000 = 74,9%: o percentual exibido arredonda, a regra não
        SubjectReport r = calcular(List.of(nota(1, "1", "10", "10")), 1000, 749);

        assertThat(r.result()).isEqualTo(ReportResult.FAILED);
    }

    @Test
    void semNotaEmAndamentoMasFaltaDemaisJaReprova() {
        assertThat(calcular(List.of(), 0, 0).result()).isEqualTo(ReportResult.IN_PROGRESS);
        assertThat(calcular(List.of(), 10, 5).result()).isEqualTo(ReportResult.FAILED);
        assertThat(calcular(List.of(), 0, 0).average()).isNull();
        assertThat(calcular(List.of(), 0, 0).attendance()).isEqualByComparingTo("100.0");
    }

    @Test
    void resultadoGeralPegaOPiorCaso() {
        SubjectReport aprovada = calcular(List.of(nota(1, "1", "10", "8")), 4, 4);
        SubjectReport recuperacao = calcular(List.of(nota(1, "1", "10", "3")), 4, 4);
        SubjectReport reprovada = calcular(List.of(nota(1, "1", "10", "9")), 4, 1);
        SubjectReport emAndamento = calcular(List.of(), 0, 0);

        assertThat(ReportCardCalculator.overall(List.of(aprovada))).isEqualTo(ReportResult.APPROVED);
        assertThat(ReportCardCalculator.overall(List.of(aprovada, emAndamento))).isEqualTo(ReportResult.IN_PROGRESS);
        assertThat(ReportCardCalculator.overall(List.of(aprovada, emAndamento, recuperacao))).isEqualTo(ReportResult.RECOVERY);
        assertThat(ReportCardCalculator.overall(List.of(aprovada, recuperacao, reprovada))).isEqualTo(ReportResult.FAILED);
        assertThat(ReportCardCalculator.overall(List.of())).isEqualTo(ReportResult.IN_PROGRESS);
    }
}
