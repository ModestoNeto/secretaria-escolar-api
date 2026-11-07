package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.ClassSubject;

import java.util.List;

public interface ClassSubjectUseCase {

    /**
     * Turma e disciplina inexistentes lançam NotFoundException; usuário que não existe, está inativo ou não é
     * PROFESSOR lança InvalidRequestException; vínculo repetido lança ConflictException.
     */
    ClassSubject link(Long classId, Long subjectId, Long teacherUserId);

    /** teacherUserId nulo lista todos os vínculos da turma; preenchido, só os desse professor. */
    List<ClassSubject> list(Long classId, Long teacherUserId);

    /** NotFoundException se a turma ou o vínculo não existirem. */
    void unlink(Long classId, Long subjectId);
}
