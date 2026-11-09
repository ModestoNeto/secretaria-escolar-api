package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepositoryPort {

    /** Id nulo cria; preenchido atualiza. Lança ConflictException se o aluno já tiver matrícula ativa no ano. */
    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(Long id);

    long countActiveByClass(Long classId);

    /** Qualquer matrícula da turma, inclusive cancelada. */
    boolean existsByClass(Long classId);

    /** Só as ativas, com nome e número de matrícula do aluno, por ordem de nome (uma consulta só, sem N+1). */
    List<EnrolledStudent> findActiveByClass(Long classId);
}
