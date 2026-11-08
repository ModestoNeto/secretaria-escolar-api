package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.exception.TooManyAttemptsException;
import com.modestoneto.secretaria.domain.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<StandardError> unauthorized(UnauthorizedException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<StandardError> tooManyAttempts(TooManyAttemptsException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()))
                .body(new StandardError(Instant.now(), 429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<StandardError> notFound(NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<StandardError> conflict(ConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<StandardError> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted().reduce((a, b) -> a + "; " + b).orElse("Requisição inválida");
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<StandardError> invalidRequest(InvalidRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<StandardError> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<StandardError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<StandardError> missingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório: " + e.getParameterName(), request);
    }

    private ResponseEntity<StandardError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new StandardError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
