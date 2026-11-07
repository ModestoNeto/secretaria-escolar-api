package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface SpringDataClassSubjectRepository extends JpaRepository<ClassSubjectJpaEntity, Long> {

    List<ClassSubjectJpaEntity> findByClassIdOrderBySubjectId(Long classId);

    List<ClassSubjectJpaEntity> findByClassIdAndTeacherUserIdOrderBySubjectId(Long classId, Long teacherUserId);

    @Modifying
    @Query("delete from ClassSubjectJpaEntity cs where cs.classId = :classId and cs.subjectId = :subjectId")
    int deleteLink(@Param("classId") Long classId, @Param("subjectId") Long subjectId);
}
