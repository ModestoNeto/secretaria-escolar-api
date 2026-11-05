package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.Student;

final class StudentMapper {

    private StudentMapper() {
    }

    static Student toDomain(StudentJpaEntity e) {
        return new Student(e.getId(), e.getRegistrationNumber(), e.getFullName(), e.getBirthDate(),
                e.getGuardianName(), e.getGuardianPhone(), e.getEmail(), e.isActive());
    }

    static StudentJpaEntity toEntity(Student s) {
        return new StudentJpaEntity(s.id(), s.registrationNumber(), s.fullName(), s.birthDate(),
                s.guardianName(), s.guardianPhone(), s.email(), s.active());
    }
}
