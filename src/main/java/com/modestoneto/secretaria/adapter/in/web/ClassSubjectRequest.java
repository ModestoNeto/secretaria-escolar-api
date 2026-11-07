package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.NotNull;

record ClassSubjectRequest(@NotNull Long subjectId, @NotNull Long teacherUserId) {
}
