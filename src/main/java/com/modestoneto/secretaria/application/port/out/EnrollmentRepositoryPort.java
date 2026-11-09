package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepositoryPort {

    /** Id nulo cria; preenchido atualiza. Lança ConflictException se o aluno já tiver matrícula ativa no ano. */
    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(Long id);

    long countActiveByClass(Long classId);

    List<Enrollment> findActiveByClass(Long classId);
}
