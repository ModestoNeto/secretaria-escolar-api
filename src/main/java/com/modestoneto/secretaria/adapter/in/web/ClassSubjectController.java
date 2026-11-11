package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.ClassSubjectUseCase;
import com.modestoneto.secretaria.application.port.in.ClassUseCase;
import com.modestoneto.secretaria.application.port.in.SubjectUseCase;
import com.modestoneto.secretaria.domain.model.Subject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classes/{classId}/subjects")
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
class ClassSubjectController {

    private final ClassSubjectUseCase links;
    private final ClassUseCase classes;
    private final SubjectUseCase subjects;

    ClassSubjectController(ClassSubjectUseCase links, ClassUseCase classes, SubjectUseCase subjects) {
        this.links = links;
        this.classes = classes;
        this.subjects = subjects;
    }

    // Sem Location: não há GET de um vínculo isolado.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClassSubjectResponse link(@PathVariable Long classId, @Valid @RequestBody ClassSubjectRequest request) {
        return ClassSubjectResponse.from(links.link(classId, request.subjectId(), request.teacherUserId()),
                names().get(request.subjectId()));
    }

    // O professor vê só os vínculos dele, e só em turma dele (senão 404, como em GET /classes/{id}).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA','PROFESSOR')")
    List<ClassSubjectResponse> list(@PathVariable Long classId, @AuthenticationPrincipal Jwt jwt) {
        Long teacher = null;
        if (Principals.isProfessor(jwt)) {
            teacher = Principals.userId(jwt);
            classes.findByIdForTeacher(classId, teacher);
        }
        // o nome vem aqui porque o professor não pode ler GET /subjects
        Map<Long, String> names = names();
        return links.list(classId, teacher).stream()
                .map(cs -> ClassSubjectResponse.from(cs, names.get(cs.subjectId()))).toList();
    }

    // shortcut: lê a lista inteira de disciplinas (pequena, sem paginação); trocar por busca por ids se crescer
    private Map<Long, String> names() {
        return subjects.list().stream().collect(Collectors.toMap(Subject::id, Subject::name));
    }

    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlink(@PathVariable Long classId, @PathVariable Long subjectId) {
        links.unlink(classId, subjectId);
    }
}
