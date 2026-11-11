package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.AttendanceSheetEntry;
import com.modestoneto.secretaria.domain.model.AttendanceSummary;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceUseCase {

    record PresenceInput(Long enrollmentId, boolean present) {
    }

    /**
     * teacherUserId nulo é a gestão (só consulta); preenchido, o professor precisa ser o titular da disciplina,
     * senão NotFoundException (como em avaliações). Lança InvalidRequestException para data futura, matrícula
     * repetida ou que não está ativa na turma. Regrava a chamada do dia: quem já tinha registro é atualizado.
     */
    List<AttendanceSheetEntry> save(Long classId, Long subjectId, LocalDate date, Long teacherUserId, List<PresenceInput> inputs);

    /** Alunos com matrícula ativa e a presença na data (nula se ainda não registrada). */
    List<AttendanceSheetEntry> sheet(Long classId, Long subjectId, LocalDate date, Long teacherUserId);

    /** Uma linha por disciplina com aulas registradas. NotFoundException se a matrícula não existir. */
    List<AttendanceSummary> summary(Long enrollmentId);
}
