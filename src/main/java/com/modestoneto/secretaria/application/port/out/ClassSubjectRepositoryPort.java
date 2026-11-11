package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.ClassSubject;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepositoryPort {

    /** Lança ConflictException se a disciplina já estiver vinculada à turma. */
    ClassSubject save(ClassSubject classSubject);

    Optional<ClassSubject> findById(Long id);

    Optional<ClassSubject> findByClassAndSubject(Long classId, Long subjectId);

    /** Como findByClassAndSubject, mas trava a linha até o fim da transação (serializa quem grava na mesma disciplina). */
    Optional<ClassSubject> findByClassAndSubjectForUpdate(Long classId, Long subjectId);

    /** teacherUserId nulo devolve todos os vínculos da turma. */
    List<ClassSubject> findByClass(Long classId, Long teacherUserId);

    /** Devolve false se o vínculo não existia. Lança ConflictException se ele já tiver avaliações. */
    boolean delete(Long classId, Long subjectId);
}
