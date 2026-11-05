package com.modestoneto.secretaria.adapter.in.web;

import java.time.Instant;

public record StandardError(Instant timestamp, int status, String error, String message, String path) {
}
