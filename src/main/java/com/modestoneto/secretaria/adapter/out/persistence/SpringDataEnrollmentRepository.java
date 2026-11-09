package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {

    long countByClassIdAndStatus(Long classId, EnrollmentStatus status);

    boolean existsByClassId(Long classId);

    // Junta pelo id (as entidades guardam só ids): matrícula e aluno vêm na mesma consulta, sem N+1.
    @Query("select e, s from EnrollmentJpaEntity e, StudentJpaEntity s "
            + "where s.id = e.studentId and e.classId = :classId and e.status = :status "
            + "order by s.fullName asc, e.id asc")
    List<Object[]> findWithStudent(@Param("classId") Long classId, @Param("status") EnrollmentStatus status);
}
