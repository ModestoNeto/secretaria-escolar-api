package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.PageResult;
import com.modestoneto.secretaria.application.common.SortDirection;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.domain.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Override
    @Transactional(readOnly = true)
    public PageResult<Student> search(boolean includeInactive, PageRequest p) {
        Sort.Direction direction = p.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        // o desempate por id mantém a ordem estável entre as páginas quando há nomes iguais
        Sort sort = Sort.by(direction, p.sort()).and(Sort.by(direction, "id"));
        Pageable pageable = org.springframework.data.domain.PageRequest.of(p.page(), p.size(), sort);

        Page<StudentJpaEntity> page = includeInactive
                ? repository.findAll(pageable)
                : repository.findByActiveTrue(pageable);
        return new PageResult<>(page.map(StudentMapper::toDomain).getContent(),
                p.page(), p.size(), page.getTotalElements());
    }
}
