package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.SubjectRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.Subject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
class SubjectPersistenceAdapter implements SubjectRepositoryPort {

    private final SpringDataSubjectRepository repository;

    SubjectPersistenceAdapter(SpringDataSubjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public Subject save(Subject subject) {
        try {
            SubjectJpaEntity saved = repository.saveAndFlush(new SubjectJpaEntity(subject.id(), subject.name()));
            return new Subject(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Disciplina já cadastrada");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> findAllByName() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(e -> new Subject(e.getId(), e.getName())).toList();
    }
}
