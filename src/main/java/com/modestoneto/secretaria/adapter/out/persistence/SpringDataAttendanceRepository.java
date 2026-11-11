package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface SpringDataAttendanceRepository extends JpaRepository<AttendanceRecordJpaEntity, Long> {

    List<AttendanceRecordJpaEntity> findByClassSubjectIdAndLessonDate(Long classSubjectId, LocalDate lessonDate);

    // Chamada numa consulta só: matrículas ativas da turma, o aluno e a presença na aula (left join: nula se não registrada).
    @Query("select e.id, s.id, s.fullName, s.registrationNumber, r.present "
            + "from EnrollmentJpaEntity e join StudentJpaEntity s on s.id = e.studentId "
            + "left join AttendanceRecordJpaEntity r on r.enrollmentId = e.id and r.classSubjectId = :classSubjectId "
            + "and r.lessonDate = :date "
            + "where e.classId = :classId and e.status = :status "
            + "order by s.fullName asc, e.id asc")
    List<Object[]> findSheet(@Param("classSubjectId") Long classSubjectId, @Param("date") LocalDate date,
                             @Param("classId") Long classId, @Param("status") EnrollmentStatus status);

    // Frequência por disciplina numa consulta só (agregação no banco, sem carregar os registros).
    @Query("select cs.id, cs.subjectId, sub.name, count(r), sum(case when r.present = true then 1 else 0 end) "
            + "from AttendanceRecordJpaEntity r join ClassSubjectJpaEntity cs on cs.id = r.classSubjectId "
            + "join SubjectJpaEntity sub on sub.id = cs.subjectId "
            + "where r.enrollmentId = :enrollmentId "
            + "group by cs.id, cs.subjectId, sub.name order by sub.name asc, cs.id asc")
    List<Object[]> findSummary(@Param("enrollmentId") Long enrollmentId);
}
