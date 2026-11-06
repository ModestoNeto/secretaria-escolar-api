package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.SubjectUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subjects")
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
class SubjectController {

    private final SubjectUseCase subjects;

    SubjectController(SubjectUseCase subjects) {
        this.subjects = subjects;
    }

    // Sem Location: não há GET /subjects/{id}.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return SubjectResponse.from(subjects.create(request.name().trim()));
    }

    @GetMapping
    List<SubjectResponse> list() {
        return subjects.list().stream().map(SubjectResponse::from).toList();
    }
}
