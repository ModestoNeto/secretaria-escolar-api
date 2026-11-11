package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.AssessmentUseCase;
import com.modestoneto.secretaria.application.port.in.AssessmentUseCase.AssessmentData;
import com.modestoneto.secretaria.application.port.in.AssessmentUseCase.GradeInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Matriz de permissões: quem lança é o professor titular; ADMIN e SECRETARIA só consultam.
// Para o professor, disciplina ou avaliação de outro professor dá 404 (como as turmas).
@RestController
class AssessmentController {

    private static final String LEITURA = "hasAnyRole('ADMIN','SECRETARIA','PROFESSOR')";

    private final AssessmentUseCase assessments;

    AssessmentController(AssessmentUseCase assessments) {
        this.assessments = assessments;
    }

    @PostMapping("/classes/{classId}/subjects/{subjectId}/assessments")
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.CREATED)
    AssessmentResponse create(@PathVariable Long classId, @PathVariable Long subjectId,
                              @Valid @RequestBody AssessmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        var data = new AssessmentData(request.name().trim(), request.term(), request.weight(), request.maxScore());
        return AssessmentResponse.from(assessments.create(classId, subjectId, Principals.userId(jwt), data));
    }

    @PutMapping("/assessments/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    AssessmentResponse update(@PathVariable Long id, @Valid @RequestBody AssessmentRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        var data = new AssessmentData(request.name().trim(), request.term(), request.weight(), request.maxScore());
        return AssessmentResponse.from(assessments.update(id, Principals.userId(jwt), data));
    }

    // Só apaga avaliação sem notas (409 se já houver).
    @DeleteMapping("/assessments/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        assessments.delete(id, Principals.userId(jwt));
    }

    @GetMapping("/classes/{classId}/subjects/{subjectId}/assessments")
    @PreAuthorize(LEITURA)
    List<AssessmentResponse> list(@PathVariable Long classId, @PathVariable Long subjectId, @AuthenticationPrincipal Jwt jwt) {
        return assessments.list(classId, subjectId, teacherOrNull(jwt)).stream().map(AssessmentResponse::from).toList();
    }

    @GetMapping("/assessments/{id}/grades")
    @PreAuthorize(LEITURA)
    List<GradeSheetResponse> sheet(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return assessments.sheet(id, teacherOrNull(jwt)).stream().map(GradeSheetResponse::from).toList();
    }

    // Grava tudo ou nada; devolve a pauta já atualizada.
    @PutMapping("/assessments/{id}/grades")
    @PreAuthorize("hasRole('PROFESSOR')")
    List<GradeSheetResponse> saveGrades(@PathVariable Long id, @Valid @RequestBody GradesRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        var inputs = request.grades().stream().map(g -> new GradeInput(g.enrollmentId(), g.score())).toList();
        return assessments.saveGrades(id, Principals.userId(jwt), inputs).stream().map(GradeSheetResponse::from).toList();
    }

    private static Long teacherOrNull(Jwt jwt) {
        return Principals.isProfessor(jwt) ? Principals.userId(jwt) : null;
    }
}
