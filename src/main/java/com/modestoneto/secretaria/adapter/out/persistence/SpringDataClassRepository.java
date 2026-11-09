package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataClassRepository extends JpaRepository<ClassJpaEntity, Long> {

    // EXISTS (e não JOIN): uma turma com várias disciplinas do mesmo professor não se repete na lista.
    @Query("select c from ClassJpaEntity c where exists "
            + "(select 1 from ClassSubjectJpaEntity cs where cs.classId = c.id and cs.teacherUserId = :teacher) "
            + "order by c.schoolYear desc, c.name asc")
    List<ClassJpaEntity> findLinkedToTeacher(@Param("teacher") Long teacher);

    @Query("select c from ClassJpaEntity c where c.schoolYear = :year and exists "
            + "(select 1 from ClassSubjectJpaEntity cs where cs.classId = c.id and cs.teacherUserId = :teacher) "
            + "order by c.name asc")
    List<ClassJpaEntity> findLinkedToTeacherInYear(@Param("teacher") Long teacher, @Param("year") int year);

    @Query("select count(cs) > 0 from ClassSubjectJpaEntity cs where cs.classId = :classId and cs.teacherUserId = :teacher")
    boolean isTeacherOf(@Param("classId") Long classId, @Param("teacher") Long teacher);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClassJpaEntity c where c.id = :id")
    Optional<ClassJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<ClassJpaEntity> findAllByOrderBySchoolYearDescNameAsc();

    List<ClassJpaEntity> findBySchoolYearOrderByNameAsc(int schoolYear);
}
