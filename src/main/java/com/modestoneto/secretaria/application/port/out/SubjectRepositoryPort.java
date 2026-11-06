package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Subject;

import java.util.List;

public interface SubjectRepositoryPort {

    /** Lança ConflictException se o nome já existir. */
    Subject save(Subject subject);

    List<Subject> findAllByName();
}
