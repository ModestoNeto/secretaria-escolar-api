package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {

    long countByClassIdAndStatus(Long classId, EnrollmentStatus status);

    List<EnrollmentJpaEntity> findByClassIdAndStatusOrderByIdAsc(Long classId, EnrollmentStatus status);
}
