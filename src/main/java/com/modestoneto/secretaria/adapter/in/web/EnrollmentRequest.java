package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.NotNull;

record EnrollmentRequest(@NotNull Long studentId, @NotNull Long classId) {
}
