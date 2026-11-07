package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.ClassSubject;

import java.util.List;

public interface ClassSubjectRepositoryPort {

    /** Lança ConflictException se a disciplina já estiver vinculada à turma. */
    ClassSubject save(ClassSubject classSubject);

    /** teacherUserId nulo devolve todos os vínculos da turma. */
    List<ClassSubject> findByClass(Long classId, Long teacherUserId);

    /** Devolve false se o vínculo não existia. */
    boolean delete(Long classId, Long subjectId);
}
