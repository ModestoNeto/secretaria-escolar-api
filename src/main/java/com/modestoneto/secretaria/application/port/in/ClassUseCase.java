package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Shift;

import java.util.List;

public interface ClassUseCase {

    record ClassData(String name, int gradeLevel, Shift shift, int schoolYear, int capacity) {
    }

    /** Lança ConflictException se já existir turma com o mesmo nome no ano. */
    SchoolClass create(ClassData data);

    SchoolClass update(Long id, ClassData data);

    SchoolClass findById(Long id);

    /** schoolYear nulo lista todas; sem paginação (lista pequena). */
    List<SchoolClass> list(Integer schoolYear);
}
