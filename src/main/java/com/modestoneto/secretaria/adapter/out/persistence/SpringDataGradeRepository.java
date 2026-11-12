package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface SpringDataGradeRepository extends JpaRepository<GradeJpaEntity, Long> {

    List<GradeJpaEntity> findByAssessmentId(Long assessmentId);

    // Todas as notas da matrícula com peso e máximo da avaliação, numa consulta só (usada no boletim).
    @Query("select a.classSubjectId, a.term, a.weight, a.maxScore, g.score "
            + "from GradeJpaEntity g join AssessmentJpaEntity a on a.id = g.assessmentId "
            + "where g.enrollmentId = :enrollmentId")
    List<Object[]> findGradeLines(@Param("enrollmentId") Long enrollmentId);

    // Pauta numa consulta só: matrículas ativas da turma, o aluno e a nota (left join: nula se ainda não lançada).
    @Query("select e.id, s.id, s.fullName, s.registrationNumber, g.score "
            + "from EnrollmentJpaEntity e join StudentJpaEntity s on s.id = e.studentId "
            + "left join GradeJpaEntity g on g.enrollmentId = e.id and g.assessmentId = :assessmentId "
            + "where e.classId = :classId and e.status = :status "
            + "order by s.fullName asc, e.id asc")
    List<Object[]> findSheet(@Param("assessmentId") Long assessmentId, @Param("classId") Long classId,
                             @Param("status") EnrollmentStatus status);
}
