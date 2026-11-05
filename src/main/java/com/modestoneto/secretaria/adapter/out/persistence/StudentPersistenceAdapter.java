package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.domain.model.Student;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Year;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
class StudentPersistenceAdapter implements StudentRepositoryPort {

    private final SpringDataStudentRepository repository;
    private final Clock clock;

    StudentPersistenceAdapter(SpringDataStudentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Student save(Student student) {
        StudentJpaEntity entity = StudentMapper.toEntity(student);
        if (student.id() != null) {
            return StudentMapper.toDomain(repository.save(entity));
        }
        // A matrícula (ano + id) precisa do id gerado, e o MySQL não aceita coluna gerada sobre
        // AUTO_INCREMENT: salva com um valor provisório único e troca na mesma transação.
        entity.setRegistrationNumber("T" + Long.toString(ThreadLocalRandom.current().nextLong(), 36));
        entity = repository.saveAndFlush(entity);
        entity.setRegistrationNumber(String.format("%d%06d", Year.now(clock).getValue(), entity.getId()));
        return StudentMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) {
        return repository.findById(id).map(StudentMapper::toDomain);
    }
}
