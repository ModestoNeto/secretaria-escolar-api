package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.Assessment;
import com.modestoneto.secretaria.domain.model.GradeSheetEntry;

import java.math.BigDecimal;
import java.util.List;

/**
 * teacherUserId preenchido = quem chama é um professor: ele só enxerga as disciplinas em que é titular
 * (qualquer outra dá NotFoundException, que o web mostra como 404). Nulo = gestão, sem filtro.
 */
public interface AssessmentUseCase {

    record AssessmentData(String name, int term, BigDecimal weight, BigDecimal maxScore) {
    }

    record GradeInput(Long enrollmentId, BigDecimal score) {
    }

    /** Disciplina não vinculada à turma (ou de outro professor): NotFoundException; nome repetido no bimestre: ConflictException. */
    Assessment create(Long classId, Long subjectId, Long teacherUserId, AssessmentData data);

    List<Assessment> list(Long classId, Long subjectId, Long teacherUserId);

    List<GradeSheetEntry> sheet(Long assessmentId, Long teacherUserId);

    /**
     * Grava as notas de uma vez (tudo ou nada) e devolve a pauta. Matrícula que não está ativa nesta turma, nota
     * acima do máximo ou matrícula repetida lançam InvalidRequestException.
     */
    List<GradeSheetEntry> saveGrades(Long assessmentId, Long teacherUserId, List<GradeInput> grades);
}
