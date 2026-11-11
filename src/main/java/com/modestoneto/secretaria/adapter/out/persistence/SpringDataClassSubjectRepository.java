package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface SpringDataClassSubjectRepository extends JpaRepository<ClassSubjectJpaEntity, Long> {

    List<ClassSubjectJpaEntity> findByClassIdOrderBySubjectId(Long classId);

    List<ClassSubjectJpaEntity> findByClassIdAndTeacherUserIdOrderBySubjectId(Long classId, Long teacherUserId);

    Optional<ClassSubjectJpaEntity> findByClassIdAndSubjectId(Long classId, Long subjectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cs from ClassSubjectJpaEntity cs where cs.classId = :classId and cs.subjectId = :subjectId")
    Optional<ClassSubjectJpaEntity> findByClassAndSubjectForUpdate(@Param("classId") Long classId, @Param("subjectId") Long subjectId);

    @Modifying
    @Query("delete from ClassSubjectJpaEntity cs where cs.classId = :classId and cs.subjectId = :subjectId")
    int deleteLink(@Param("classId") Long classId, @Param("subjectId") Long subjectId);
}
