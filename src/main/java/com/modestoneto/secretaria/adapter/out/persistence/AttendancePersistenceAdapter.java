package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.AttendanceRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.AttendanceRecord;
import com.modestoneto.secretaria.domain.model.AttendanceSheetEntry;
import com.modestoneto.secretaria.domain.model.AttendanceSummary;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
class AttendancePersistenceAdapter implements AttendanceRepositoryPort {

    private final SpringDataAttendanceRepository repository;

    AttendancePersistenceAdapter(SpringDataAttendanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AttendanceRecord> saveAll(List<AttendanceRecord> records) {
        try {
            return repository.saveAllAndFlush(records.stream()
                            .map(r -> new AttendanceRecordJpaEntity(r.id(), r.classSubjectId(), r.lessonDate(), r.enrollmentId(), r.present()))
                            .toList())
                    .stream().map(AttendancePersistenceAdapter::toDomain).toList();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Chamada alterada ao mesmo tempo por outra pessoa; tente de novo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecord> findByLesson(Long classSubjectId, LocalDate date) {
        return repository.findByClassSubjectIdAndLessonDate(classSubjectId, date).stream()
                .map(AttendancePersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSheetEntry> findSheet(Long classSubjectId, LocalDate date, Long classId) {
        return repository.findSheet(classSubjectId, date, classId, EnrollmentStatus.ACTIVE).stream()
                .map(r -> new AttendanceSheetEntry((Long) r[0], (Long) r[1], (String) r[2], (String) r[3], (Boolean) r[4]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummary> findSummary(Long enrollmentId) {
        return repository.findSummary(enrollmentId).stream()
                .map(r -> new AttendanceSummary((Long) r[0], (Long) r[1], (String) r[2], (Long) r[3], (Long) r[4]))
                .toList();
    }

    private static AttendanceRecord toDomain(AttendanceRecordJpaEntity e) {
        return new AttendanceRecord(e.getId(), e.getClassSubjectId(), e.getLessonDate(), e.getEnrollmentId(), e.isPresent());
    }
}
