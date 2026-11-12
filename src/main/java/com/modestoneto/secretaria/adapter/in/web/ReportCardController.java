package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.ReportCardUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Gestão vê o boletim de qualquer aluno; o professor só o de alunos de turmas em que é titular (senão 404).
@RestController
class ReportCardController {

    private final ReportCardUseCase reportCards;

    ReportCardController(ReportCardUseCase reportCards) {
        this.reportCards = reportCards;
    }

    @GetMapping("/students/{studentId}/report-card")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA','PROFESSOR')")
    ReportCardResponse get(@PathVariable Long studentId, @RequestParam(required = false) Integer schoolYear,
                           @AuthenticationPrincipal Jwt jwt) {
        Long teacher = Principals.isProfessor(jwt) ? Principals.userId(jwt) : null;
        return ReportCardResponse.from(reportCards.get(studentId, schoolYear, teacher));
    }
}
