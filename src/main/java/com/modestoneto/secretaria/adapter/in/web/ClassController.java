package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.ClassUseCase;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/classes")
// PROFESSOR passa a "consultar as suas" quando o vínculo professor-disciplina existir.
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
class ClassController {

    private final ClassUseCase classes;

    ClassController(ClassUseCase classes) {
        this.classes = classes;
    }

    @PostMapping
    ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest request) {
        SchoolClass created = classes.create(request.toData());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(ClassResponse.from(created));
    }

    @GetMapping
    List<ClassResponse> list(@RequestParam(required = false) Integer schoolYear) {
        return classes.list(schoolYear).stream().map(ClassResponse::from).toList();
    }

    @GetMapping("/{id}")
    ClassResponse findById(@PathVariable Long id) {
        return ClassResponse.from(classes.findById(id));
    }

    @PutMapping("/{id}")
    ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return ClassResponse.from(classes.update(id, request.toData()));
    }
}
