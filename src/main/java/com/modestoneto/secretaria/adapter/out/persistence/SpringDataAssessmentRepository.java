package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataAssessmentRepository extends JpaRepository<AssessmentJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssessmentJpaEntity a where a.id = :id")
    Optional<AssessmentJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<AssessmentJpaEntity> findByClassSubjectIdOrderByTermAscNameAsc(Long classSubjectId);
}
