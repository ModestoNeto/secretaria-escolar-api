package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.ReportCardRepositoryPort;
import com.modestoneto.secretaria.domain.model.GradeLine;
import com.modestoneto.secretaria.domain.model.SubjectRef;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
class ReportCardPersistenceAdapter implements ReportCardRepositoryPort {

    private final SpringDataClassSubjectRepository classSubjects;
    private final SpringDataGradeRepository grades;

    ReportCardPersistenceAdapter(SpringDataClassSubjectRepository classSubjects, SpringDataGradeRepository grades) {
        this.classSubjects = classSubjects;
        this.grades = grades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectRef> findSubjects(Long classId) {
        return classSubjects.findSubjectRefs(classId).stream()
                .map(r -> new SubjectRef((Long) r[0], (Long) r[1], (String) r[2])).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeLine> findGradeLines(Long enrollmentId) {
        return grades.findGradeLines(enrollmentId).stream()
                .map(r -> new GradeLine((Long) r[0], (Integer) r[1], (BigDecimal) r[2], (BigDecimal) r[3], (BigDecimal) r[4]))
                .toList();
    }
}
