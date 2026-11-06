package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.SortDirection;
import com.modestoneto.secretaria.application.port.in.StudentUseCase;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.model.Student;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/students")
// PROFESSOR fica sem acesso até existirem turmas (a posse "só os das suas turmas" vem depois).
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
class StudentController {

    private final StudentUseCase students;

    StudentController(StudentUseCase students) {
        this.students = students;
    }

    @PostMapping
    ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        Student created = students.create(request.toData());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(StudentResponse.from(created));
    }

    @GetMapping
    PageResponse<StudentResponse> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        String[] partes = sort.split(",");
        SortDirection direction = partes.length > 1 ? parseDirection(partes[1]) : SortDirection.ASC;
        var result = students.search(q, includeInactive, new PageRequest(page, size, partes[0], direction));
        return PageResponse.from(result.map(StudentResponse::from));
    }

    private static SortDirection parseDirection(String valor) {
        try {
            return SortDirection.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("direção deve ser asc ou desc");
        }
    }

    @GetMapping("/{id}")
    StudentResponse findById(@PathVariable Long id) {
        return StudentResponse.from(students.findById(id));
    }

    @PutMapping("/{id}")
    StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return StudentResponse.from(students.update(id, request.toData()));
    }

    // Desativa (não apaga): o histórico de matrículas e notas depende do aluno.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable Long id) {
        students.deactivate(id);
    }
}
