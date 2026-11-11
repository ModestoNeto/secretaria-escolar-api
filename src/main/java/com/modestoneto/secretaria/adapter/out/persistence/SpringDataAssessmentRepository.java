package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataAssessmentRepository extends JpaRepository<AssessmentJpaEntity, Long> {

    List<AssessmentJpaEntity> findByClassSubjectIdOrderByTermAscNameAsc(Long classSubjectId);
}
