package com.modestoneto.secretaria.domain.model;

/** Uma matrícula junto com os dados do aluno que a lista da turma precisa mostrar. */
public record EnrolledStudent(Enrollment enrollment, String studentName, String registrationNumber) {
}
