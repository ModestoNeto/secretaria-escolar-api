package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Sem associações @ManyToOne: o vínculo guarda só os ids (os FKs ficam no banco) e a entidade nunca
// carrega turma, disciplina ou usuário, então não há LAZY nem N+1 a vigiar aqui.
@Entity
@Table(name = "class_subjects")
class ClassSubjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "teacher_user_id", nullable = false)
    private Long teacherUserId;

    protected ClassSubjectJpaEntity() {
    }

    ClassSubjectJpaEntity(Long id, Long classId, Long subjectId, Long teacherUserId) {
        this.id = id;
        this.classId = classId;
        this.subjectId = subjectId;
        this.teacherUserId = teacherUserId;
    }

    Long getId() { return id; }
    Long getClassId() { return classId; }
    Long getSubjectId() { return subjectId; }
    Long getTeacherUserId() { return teacherUserId; }
}
