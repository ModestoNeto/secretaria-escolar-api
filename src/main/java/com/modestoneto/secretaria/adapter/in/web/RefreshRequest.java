package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record RefreshRequest(@NotBlank String refreshToken) {
}
