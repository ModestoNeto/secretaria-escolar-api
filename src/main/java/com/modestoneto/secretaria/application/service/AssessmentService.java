package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.AssessmentUseCase;
import com.modestoneto.secretaria.application.port.out.AssessmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.Assessment;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.Grade;
import com.modestoneto.secretaria.domain.model.GradeSheetEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentService implements AssessmentUseCase {

    private final AssessmentRepositoryPort assessments;
    private final ClassSubjectRepositoryPort links;
    private final EnrollmentRepositoryPort enrollments;

    public AssessmentService(AssessmentRepositoryPort assessments, ClassSubjectRepositoryPort links,
                             EnrollmentRepositoryPort enrollments) {
        this.assessments = assessments;
        this.links = links;
        this.enrollments = enrollments;
    }

    @Override
    public Assessment create(Long classId, Long subjectId, Long teacherUserId, AssessmentData d) {
        ClassSubject link = linkOf(classId, subjectId, teacherUserId);
        return assessments.save(new Assessment(null, link.id(), d.name(), d.term(), d.weight(), d.maxScore()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> list(Long classId, Long subjectId, Long teacherUserId) {
        return assessments.findByClassSubject(linkOf(classId, subjectId, teacherUserId).id());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeSheetEntry> sheet(Long assessmentId, Long teacherUserId) {
        Assessment assessment = findAssessment(assessmentId);
        return assessments.findSheet(assessmentId, owned(assessment, teacherUserId).classId());
    }

    @Override
    public List<GradeSheetEntry> saveGrades(Long assessmentId, Long teacherUserId, List<GradeInput> inputs) {
        // Trava a avaliação ANTES de qualquer leitura comum (REPEATABLE READ: a primeira leitura fixa o "retrato"):
        // dois lançamentos simultâneos passam um de cada vez, e o segundo já enxerga as notas do primeiro e as
        // atualiza em vez de tentar inserir de novo. O UNIQUE do banco continua como última barreira.
        Assessment assessment = lock(assessmentId);
        Long classId = owned(assessment, teacherUserId).classId();

        Set<Long> active = enrollments.findActiveByClass(classId).stream()
                .map(EnrolledStudent::enrollment).map(e -> e.id()).collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        for (GradeInput in : inputs) {
            if (!seen.add(in.enrollmentId())) {
                throw new InvalidRequestException("Matrícula repetida: " + in.enrollmentId());
            }
            if (!active.contains(in.enrollmentId())) {
                throw new InvalidRequestException("Matrícula não está ativa nesta turma: " + in.enrollmentId());
            }
            if (in.score().compareTo(assessment.maxScore()) > 0) {
                throw new InvalidRequestException("Nota acima do máximo da avaliação (" + assessment.maxScore().toPlainString() + ")");
            }
        }

        Map<Long, Long> existing = assessments.findGrades(assessmentId).stream()
                .collect(Collectors.toMap(Grade::enrollmentId, Grade::id));
        assessments.saveGrades(inputs.stream()
                .map(in -> new Grade(existing.get(in.enrollmentId()), assessmentId, in.enrollmentId(), in.score()))
                .toList());
        return assessments.findSheet(assessmentId, classId);
    }

    @Override
    public Assessment update(Long assessmentId, Long teacherUserId, AssessmentData d) {
        Assessment atual = lock(assessmentId);
        owned(atual, teacherUserId);
        // a nota máxima não pode ficar abaixo de uma nota já lançada (o lock impede um lançamento no meio)
        if (assessments.findGrades(assessmentId).stream().anyMatch(g -> g.score().compareTo(d.maxScore()) > 0)) {
            throw new ConflictException("Há notas acima do novo máximo da avaliação");
        }
        return assessments.save(new Assessment(atual.id(), atual.classSubjectId(), d.name(), d.term(), d.weight(), d.maxScore()));
    }

    @Override
    public void delete(Long assessmentId, Long teacherUserId) {
        Assessment atual = lock(assessmentId);
        owned(atual, teacherUserId);
        if (!assessments.findGrades(assessmentId).isEmpty()) {
            throw new ConflictException("Avaliação com notas: não é possível apagar");
        }
        assessments.delete(assessmentId);
    }

    private Assessment lock(Long id) {
        return assessments.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Avaliação não encontrada: " + id));
    }

    private Assessment findAssessment(Long id) {
        return assessments.findById(id).orElseThrow(() -> new NotFoundException("Avaliação não encontrada: " + id));
    }

    // Professor que não é o titular recebe o mesmo 404 de quem pede algo que não existe (não revela que existe).
    private ClassSubject linkOf(Long classId, Long subjectId, Long teacherUserId) {
        return links.findByClassAndSubject(classId, subjectId)
                .filter(l -> teacherUserId == null || teacherUserId.equals(l.teacherUserId()))
                .orElseThrow(() -> new NotFoundException("Disciplina não vinculada a esta turma"));
    }

    private ClassSubject owned(Assessment assessment, Long teacherUserId) {
        return links.findById(assessment.classSubjectId())
                .filter(l -> teacherUserId == null || teacherUserId.equals(l.teacherUserId()))
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada: " + assessment.id()));
    }
}
