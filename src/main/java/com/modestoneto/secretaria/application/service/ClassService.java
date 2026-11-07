package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.ClassUseCase;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClassService implements ClassUseCase {

    private final ClassRepositoryPort classes;

    public ClassService(ClassRepositoryPort classes) {
        this.classes = classes;
    }

    @Override
    public SchoolClass create(ClassData d) {
        return classes.save(new SchoolClass(null, d.name(), d.gradeLevel(), d.shift(), d.schoolYear(), d.capacity()));
    }

    @Override
    public SchoolClass update(Long id, ClassData d) {
        SchoolClass atual = findById(id);
        return classes.save(new SchoolClass(atual.id(), d.name(), d.gradeLevel(), d.shift(), d.schoolYear(), d.capacity()));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClass findById(Long id) {
        return classes.findById(id).orElseThrow(() -> new NotFoundException("Turma não encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClass> list(Integer schoolYear) {
        return classes.findAll(schoolYear);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClass> listForTeacher(Long teacherUserId, Integer schoolYear) {
        return classes.findAllByTeacher(teacherUserId, schoolYear);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClass findByIdForTeacher(Long id, Long teacherUserId) {
        if (!classes.isTeacherOf(id, teacherUserId)) {
            // 404 e não 403: não revela que a turma existe
            throw new NotFoundException("Turma não encontrada: " + id);
        }
        return findById(id);
    }
}
