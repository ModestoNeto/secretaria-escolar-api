package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    @Transactional(readOnly = true)
    public Optional<ClassSubject> findById(Long id) {
        return repository.findById(id).map(ClassSubjectPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassSubject> findByClassAndSubject(Long classId, Long subjectId) {
        return repository.findByClassIdAndSubjectId(classId, subjectId).map(ClassSubjectPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ClassSubject> findByClassAndSubjectForUpdate(Long classId, Long subjectId) {
        return repository.findByClassAndSubjectForUpdate(classId, subjectId).map(ClassSubjectPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public boolean delete(Long classId, Long subjectId) {
        try {
            return repository.deleteLink(classId, subjectId) > 0;
        } catch (DataIntegrityViolationException e) {
            // a FK de assessments segura o vínculo: apagá-lo deixaria avaliações e notas sem dono
            throw new ConflictException("Disciplina com avaliações: não é possível desvincular");
        }
    }

    private static ClassSubject toDomain(ClassSubjectJpaEntity e) {
        return new ClassSubject(e.getId(), e.getClassId(), e.getSubjectId(), e.getTeacherUserId());
    }
}
