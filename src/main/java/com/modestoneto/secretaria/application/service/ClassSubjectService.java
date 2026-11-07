package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.ClassSubjectUseCase;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.SubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClassSubjectService implements ClassSubjectUseCase {

    private final ClassSubjectRepositoryPort links;
    private final ClassRepositoryPort classes;
    private final SubjectRepositoryPort subjects;
    private final UserRepositoryPort users;

    public ClassSubjectService(ClassSubjectRepositoryPort links, ClassRepositoryPort classes,
                               SubjectRepositoryPort subjects, UserRepositoryPort users) {
        this.links = links;
        this.classes = classes;
        this.subjects = subjects;
        this.users = users;
    }

    @Override
    public ClassSubject link(Long classId, Long subjectId, Long teacherUserId) {
        requireClass(classId);
        subjects.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Disciplina não encontrada: " + subjectId));
        User teacher = users.findById(teacherUserId)
                .orElseThrow(() -> new InvalidRequestException("Professor não encontrado: " + teacherUserId));
        if (teacher.role() != Role.PROFESSOR) {
            throw new InvalidRequestException("Usuário não é professor");
        }
        if (!teacher.active()) {
            throw new InvalidRequestException("Professor inativo");
        }
        return links.save(new ClassSubject(null, classId, subjectId, teacherUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubject> list(Long classId, Long teacherUserId) {
        requireClass(classId);
        return links.findByClass(classId, teacherUserId);
    }

    @Override
    public void unlink(Long classId, Long subjectId) {
        requireClass(classId);
        if (!links.delete(classId, subjectId)) {
            throw new NotFoundException("Vínculo não encontrado");
        }
    }

    private void requireClass(Long classId) {
        classes.findById(classId).orElseThrow(() -> new NotFoundException("Turma não encontrada: " + classId));
    }
}
