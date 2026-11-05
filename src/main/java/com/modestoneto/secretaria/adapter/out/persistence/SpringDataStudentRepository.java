package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataStudentRepository extends JpaRepository<StudentJpaEntity, Long> {
}
