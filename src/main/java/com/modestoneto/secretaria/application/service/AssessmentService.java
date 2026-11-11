package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.AssessmentUseCase;
import com.modestoneto.secretaria.application.port.out.AssessmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
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
        Assessment assessment = findAssessment(assessmentId);
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
