package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.PageResult;
import com.modestoneto.secretaria.domain.model.Student;

import java.time.LocalDate;
import java.util.Set;

public interface StudentUseCase {

    /** Campos permitidos na ordenação; qualquer outro vira 400. */
    Set<String> SORT_FIELDS = Set.of("fullName", "registrationNumber", "birthDate");

    /** cpf: os 11 dígitos, já normalizados e validados por Cpf.normalize. */
    record StudentData(String fullName, LocalDate birthDate, String guardianName,
                       String guardianPhone, String email, String cpf) {
    }

    /** Lança ConflictException se o CPF já pertencer a outro aluno. */
    Student create(StudentData data);

    Student update(Long id, StudentData data);

    Student findById(Long id);

    /** Desativa sem apagar; repetir a chamada não muda nada. */
    void deactivate(Long id);

    /**
     * q: um CPF (com ou sem máscara, inteiro ou o começo dele) procura pelo CPF; qualquer outro texto é um
     * trecho do nome, sem distinguir acento nem caixa (vazio devolve todos).
     */
    PageResult<Student> search(String q, boolean includeInactive, PageRequest pageRequest);
}
