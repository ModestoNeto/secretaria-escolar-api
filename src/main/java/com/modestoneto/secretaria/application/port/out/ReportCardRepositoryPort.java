package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.GradeLine;
import com.modestoneto.secretaria.domain.model.SubjectRef;

import java.util.List;

/** Leituras do boletim: cada uma é uma consulta só, não importa quantas disciplinas ou avaliações existam. */
public interface ReportCardRepositoryPort {

    /** Disciplinas da turma com o nome, por nome. */
    List<SubjectRef> findSubjects(Long classId);

    /** Todas as notas da matrícula, com peso e máximo da avaliação. */
    List<GradeLine> findGradeLines(Long enrollmentId);
}
