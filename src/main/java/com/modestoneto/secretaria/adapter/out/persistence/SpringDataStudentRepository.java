package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataStudentRepository extends JpaRepository<StudentJpaEntity, Long> {

    Page<StudentJpaEntity> findByActiveTrue(Pageable pageable);
}
