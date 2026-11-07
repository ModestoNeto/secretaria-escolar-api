package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepositoryPort {

    Optional<Subject> findById(Long id);


    /** Lança ConflictException se o nome já existir. */
    Subject save(Subject subject);

    List<Subject> findAllByName();
}
