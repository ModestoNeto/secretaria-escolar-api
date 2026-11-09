package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.Enrollment;

import java.util.List;

public interface EnrollmentUseCase {

    /**
     * Turma ou aluno inexistentes lançam NotFoundException; aluno inativo lança InvalidRequestException;
     * aluno que já tem matrícula ativa no ano da turma ou turma sem vagas lançam ConflictException.
     */
    Enrollment enroll(Long studentId, Long classId);

    /** Cancela a matrícula (idempotente: cancelar de novo não muda nada). NotFoundException se não existir. */
    void cancel(Long enrollmentId);

    /** Só as matrículas ativas da turma, com os dados do aluno, por ordem de nome. NotFoundException se a turma não existir. */
    List<EnrolledStudent> listActive(Long classId);
}
