package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.PageResult;
import com.modestoneto.secretaria.domain.model.Student;

import java.util.Optional;

public interface StudentRepositoryPort {

    /**
     * Id nulo cria o aluno e gera a matrícula; id preenchido atualiza.
     * Lança ConflictException se o CPF já pertencer a outro aluno.
     */
    Student save(Student student);

    Optional<Student> findById(Long id);

    PageResult<Student> search(String q, boolean includeInactive, PageRequest pageRequest);

    /** Alunos cujo CPF começa com os dígitos dados (11 dígitos é a busca exata). */
    PageResult<Student> searchByCpf(String cpfPrefix, boolean includeInactive, PageRequest pageRequest);
}
