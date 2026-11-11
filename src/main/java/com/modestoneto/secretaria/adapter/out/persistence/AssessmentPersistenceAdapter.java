package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.AssessmentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.Assessment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import com.modestoneto.secretaria.domain.model.Grade;
import com.modestoneto.secretaria.domain.model.GradeSheetEntry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
class AssessmentPersistenceAdapter implements AssessmentRepositoryPort {

    private final SpringDataAssessmentRepository assessments;
    private final SpringDataGradeRepository grades;

    AssessmentPersistenceAdapter(SpringDataAssessmentRepository assessments, SpringDataGradeRepository grades) {
        this.assessments = assessments;
        this.grades = grades;
    }

    @Override
    public Assessment save(Assessment a) {
        try {
            return toDomain(assessments.saveAndFlush(new AssessmentJpaEntity(
                    a.id(), a.classSubjectId(), a.name(), a.term(), a.weight(), a.maxScore())));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Já existe avaliação com este nome neste bimestre");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assessment> findById(Long id) {
        return assessments.findById(id).map(AssessmentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> findByClassSubject(Long classSubjectId) {
        return assessments.findByClassSubjectIdOrderByTermAscNameAsc(classSubjectId).stream()
                .map(AssessmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Grade> saveGrades(List<Grade> list) {
        try {
            return grades.saveAllAndFlush(list.stream()
                            .map(g -> new GradeJpaEntity(g.id(), g.assessmentId(), g.enrollmentId(), g.score())).toList())
                    .stream().map(AssessmentPersistenceAdapter::toDomain).toList();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Notas alteradas ao mesmo tempo por outra pessoa; tente de novo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findGrades(Long assessmentId) {
        return grades.findByAssessmentId(assessmentId).stream().map(AssessmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeSheetEntry> findSheet(Long assessmentId, Long classId) {
        return grades.findSheet(assessmentId, classId, EnrollmentStatus.ACTIVE).stream()
                .map(r -> new GradeSheetEntry((Long) r[0], (Long) r[1], (String) r[2], (String) r[3], (BigDecimal) r[4]))
                .toList();
    }

    private static Assessment toDomain(AssessmentJpaEntity e) {
        return new Assessment(e.getId(), e.getClassSubjectId(), e.getName(), e.getTerm(), e.getWeight(), e.getMaxScore());
    }

    private static Grade toDomain(GradeJpaEntity e) {
        return new Grade(e.getId(), e.getAssessmentId(), e.getEnrollmentId(), e.getScore());
    }
}
