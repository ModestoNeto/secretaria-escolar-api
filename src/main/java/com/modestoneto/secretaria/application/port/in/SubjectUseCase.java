package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.Subject;

import java.util.List;

public interface SubjectUseCase {

    /** Lança ConflictException se a disciplina já existir (sem distinguir acento nem caixa). */
    Subject create(String name);

    /** Ordenadas por nome; sem paginação (lista pequena). */
    List<Subject> list();
}
