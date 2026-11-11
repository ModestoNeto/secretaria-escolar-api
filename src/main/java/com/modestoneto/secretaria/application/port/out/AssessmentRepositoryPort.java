package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Assessment;
import com.modestoneto.secretaria.domain.model.Grade;
import com.modestoneto.secretaria.domain.model.GradeSheetEntry;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepositoryPort {

    /** Lança ConflictException se o vínculo já tiver avaliação com o mesmo nome no bimestre. */
    Assessment save(Assessment assessment);

    Optional<Assessment> findById(Long id);

    /** Por bimestre e nome. */
    List<Assessment> findByClassSubject(Long classSubjectId);

    /** Id nulo cria; preenchido atualiza. Lança ConflictException se duas gravações disputarem a mesma nota. */
    List<Grade> saveGrades(List<Grade> grades);

    List<Grade> findGrades(Long assessmentId);

    /** Alunos com matrícula ativa na turma e a nota deles na avaliação, por ordem de nome (uma consulta só). */
    List<GradeSheetEntry> findSheet(Long assessmentId, Long classId);
}
