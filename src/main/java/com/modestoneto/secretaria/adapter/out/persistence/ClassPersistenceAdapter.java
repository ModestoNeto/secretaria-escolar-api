package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
class ClassPersistenceAdapter implements ClassRepositoryPort {

    private final SpringDataClassRepository repository;

    ClassPersistenceAdapter(SpringDataClassRepository repository) {
        this.repository = repository;
    }

    @Override
    public SchoolClass save(SchoolClass c) {
        try {
            return toDomain(repository.saveAndFlush(
                    new ClassJpaEntity(c.id(), c.name(), c.gradeLevel(), c.shift(), c.schoolYear(), c.capacity())));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Já existe turma com esse nome neste ano");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SchoolClass> findById(Long id) {
        return repository.findById(id).map(ClassPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClass> findAll(Integer schoolYear) {
        var entities = schoolYear == null
                ? repository.findAllByOrderBySchoolYearDescNameAsc()
                : repository.findBySchoolYearOrderByNameAsc(schoolYear);
        return entities.stream().map(ClassPersistenceAdapter::toDomain).toList();
    }

    private static SchoolClass toDomain(ClassJpaEntity e) {
        return new SchoolClass(e.getId(), e.getName(), e.getGradeLevel(), e.getShift(), e.getSchoolYear(), e.getCapacity());
    }
}
