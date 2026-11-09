package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.EnrollmentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Matrículas são da gestão (matriz de permissões); o acesso do professor às da sua turma fica para depois.
@RestController
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
class EnrollmentController {

    private final EnrollmentUseCase enrollments;

    EnrollmentController(EnrollmentUseCase enrollments) {
        this.enrollments = enrollments;
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request) {
        return EnrollmentResponse.from(enrollments.enroll(request.studentId(), request.classId()));
    }

    // Cancela (a matrícula continua no banco); repetir o pedido também dá 204.
    @DeleteMapping("/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable Long id) {
        enrollments.cancel(id);
    }

    @GetMapping("/classes/{classId}/enrollments")
    List<EnrollmentResponse> listByClass(@PathVariable Long classId) {
        return enrollments.listActive(classId).stream().map(EnrollmentResponse::from).toList();
    }
}
