package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SubjectRequest(@NotBlank @Size(max = 80) String name) {
}
