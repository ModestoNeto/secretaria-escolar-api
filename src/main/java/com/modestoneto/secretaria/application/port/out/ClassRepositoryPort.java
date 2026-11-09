package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.SchoolClass;

import java.util.List;
import java.util.Optional;

public interface ClassRepositoryPort {

    /** Id nulo cria; preenchido atualiza. Lança ConflictException se o nome já existir no ano. */
    SchoolClass save(SchoolClass schoolClass);

    Optional<SchoolClass> findById(Long id);

    /** Como findById, mas trava a linha da turma até o fim da transação (serializa as matrículas concorrentes). */
    Optional<SchoolClass> findByIdForUpdate(Long id);

    /** schoolYear nulo devolve todas; ordenadas por ano (mais recente primeiro) e nome. */
    List<SchoolClass> findAll(Integer schoolYear);

    /** Só as turmas em que o professor tem alguma disciplina vinculada; schoolYear nulo devolve todos os anos. */
    List<SchoolClass> findAllByTeacher(Long teacherUserId, Integer schoolYear);

    boolean isTeacherOf(Long classId, Long teacherUserId);
}
