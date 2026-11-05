package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
