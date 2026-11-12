package com.modestoneto.secretaria.domain.model;

/** Disciplina de uma turma: o id do vínculo, o id e o nome da disciplina. */
public record SubjectRef(Long classSubjectId, Long subjectId, String subjectName) {
}
