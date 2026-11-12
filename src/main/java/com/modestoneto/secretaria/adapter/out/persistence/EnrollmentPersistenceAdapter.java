package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
class EnrollmentPersistenceAdapter implements EnrollmentRepositoryPort {

    private final SpringDataEnrollmentRepository repository;

    EnrollmentPersistenceAdapter(SpringDataEnrollmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Enrollment save(Enrollment e) {
        try {
            return toDomain(repository.saveAndFlush(new EnrollmentJpaEntity(
                    e.id(), e.studentId(), e.classId(), e.schoolYear(), e.status(), e.enrolledAt())));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Aluno já matriculado neste ano letivo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Enrollment> findById(Long id) {
        return repository.findById(id).map(EnrollmentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Enrollment> findActiveByStudentAndYear(Long studentId, int schoolYear) {
        return repository.findByStudentIdAndSchoolYearAndStatus(studentId, schoolYear, EnrollmentStatus.ACTIVE)
                .map(EnrollmentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByClass(Long classId) {
        return repository.countByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClass(Long classId) {
        return repository.existsByClassId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledStudent> findActiveByClass(Long classId) {
        return repository.findWithStudent(classId, EnrollmentStatus.ACTIVE).stream()
                .map(row -> {
                    var student = (StudentJpaEntity) row[1];
                    return new EnrolledStudent(toDomain((EnrollmentJpaEntity) row[0]),
                            student.getFullName(), student.getRegistrationNumber());
                }).toList();
    }

    private static Enrollment toDomain(EnrollmentJpaEntity e) {
        return new Enrollment(e.getId(), e.getStudentId(), e.getClassId(), e.getSchoolYear(), e.getStatus(), e.getEnrolledAt());
    }
}
