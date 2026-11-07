package com.modestoneto.secretaria.domain.model;

/** Quem ensina o quê em uma turma. O professor é um usuário com papel PROFESSOR. */
public record ClassSubject(Long id, Long classId, Long subjectId, Long teacherUserId) {
}
