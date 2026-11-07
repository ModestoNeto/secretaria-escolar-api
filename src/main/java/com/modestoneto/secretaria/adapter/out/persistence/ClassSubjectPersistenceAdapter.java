package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
class ClassSubjectPersistenceAdapter implements ClassSubjectRepositoryPort {

    private final SpringDataClassSubjectRepository repository;

    ClassSubjectPersistenceAdapter(SpringDataClassSubjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public ClassSubject save(ClassSubject cs) {
        try {
            return toDomain(repository.saveAndFlush(
                    new ClassSubjectJpaEntity(cs.id(), cs.classId(), cs.subjectId(), cs.teacherUserId())));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Disciplina já vinculada a esta turma");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubject> findByClass(Long classId, Long teacherUserId) {
        var entities = teacherUserId == null
                ? repository.findByClassIdOrderBySubjectId(classId)
                : repository.findByClassIdAndTeacherUserIdOrderBySubjectId(classId, teacherUserId);
        return entities.stream().map(ClassSubjectPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean delete(Long classId, Long subjectId) {
        return repository.deleteLink(classId, subjectId) > 0;
    }

    private static ClassSubject toDomain(ClassSubjectJpaEntity e) {
        return new ClassSubject(e.getId(), e.getClassId(), e.getSubjectId(), e.getTeacherUserId());
    }
}
