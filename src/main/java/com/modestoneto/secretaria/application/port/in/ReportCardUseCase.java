package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.ReportCard;

public interface ReportCardUseCase {

    /**
     * Boletim da matrícula ativa do aluno no ano (schoolYear nulo é o ano atual). NotFoundException se o aluno não
     * existir, não tiver matrícula ativa no ano ou, para o professor (teacherUserId preenchido), a turma não for dele.
     */
    ReportCard get(Long studentId, Integer schoolYear, Long teacherUserId);
}
