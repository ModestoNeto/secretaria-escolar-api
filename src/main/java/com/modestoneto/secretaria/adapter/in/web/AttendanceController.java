package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.AttendanceUseCase;
import com.modestoneto.secretaria.application.port.in.AttendanceUseCase.PresenceInput;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Matriz de permissões: quem registra é o professor titular; ADMIN e SECRETARIA só consultam.
// Para o professor, disciplina de outro professor dá 404 (como avaliações e turmas).
@RestController
class AttendanceController {

    private static final String LEITURA = "hasAnyRole('ADMIN','SECRETARIA','PROFESSOR')";

    private final AttendanceUseCase attendance;

    AttendanceController(AttendanceUseCase attendance) {
        this.attendance = attendance;
    }

    // Grava a chamada da aula (tudo ou nada; regravar a mesma data atualiza) e devolve a chamada já atualizada.
    @PutMapping("/classes/{classId}/subjects/{subjectId}/attendance/{date}")
    @PreAuthorize("hasRole('PROFESSOR')")
    List<AttendanceSheetResponse> save(@PathVariable Long classId, @PathVariable Long subjectId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @Valid @RequestBody AttendanceRequest request, @AuthenticationPrincipal Jwt jwt) {
        var inputs = request.records().stream().map(r -> new PresenceInput(r.enrollmentId(), r.present())).toList();
        return attendance.save(classId, subjectId, date, Principals.userId(jwt), inputs).stream()
                .map(AttendanceSheetResponse::from).toList();
    }

    @GetMapping("/classes/{classId}/subjects/{subjectId}/attendance/{date}")
    @PreAuthorize(LEITURA)
    List<AttendanceSheetResponse> sheet(@PathVariable Long classId, @PathVariable Long subjectId,
                                        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @AuthenticationPrincipal Jwt jwt) {
        Long teacher = Principals.isProfessor(jwt) ? Principals.userId(jwt) : null;
        return attendance.sheet(classId, subjectId, date, teacher).stream().map(AttendanceSheetResponse::from).toList();
    }

    @GetMapping("/enrollments/{enrollmentId}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    List<AttendanceSummaryResponse> summary(@PathVariable Long enrollmentId) {
        return attendance.summary(enrollmentId).stream().map(AttendanceSummaryResponse::from).toList();
    }
}
