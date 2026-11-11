package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.AttendanceUseCase;
import com.modestoneto.secretaria.application.port.out.AttendanceRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.AttendanceRecord;
import com.modestoneto.secretaria.domain.model.AttendanceSheetEntry;
import com.modestoneto.secretaria.domain.model.AttendanceSummary;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService implements AttendanceUseCase {

    private final AttendanceRepositoryPort attendance;
    private final ClassSubjectRepositoryPort links;
    private final EnrollmentRepositoryPort enrollments;
    private final Clock clock;

    public AttendanceService(AttendanceRepositoryPort attendance, ClassSubjectRepositoryPort links,
                             EnrollmentRepositoryPort enrollments, Clock clock) {
        this.attendance = attendance;
        this.links = links;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Override
    public List<AttendanceSheetEntry> save(Long classId, Long subjectId, LocalDate date, Long teacherUserId,
                                           List<PresenceInput> inputs) {
        // Trava o vínculo ANTES de qualquer leitura comum (REPEATABLE READ: a primeira leitura fixa o "retrato"):
        // duas chamadas simultâneas da mesma disciplina passam uma de cada vez, e a segunda já enxerga os registros
        // da primeira e os atualiza. O UNIQUE do banco continua como última barreira.
        ClassSubject link = links.findByClassAndSubjectForUpdate(classId, subjectId)
                .filter(l -> teacherUserId == null || teacherUserId.equals(l.teacherUserId()))
                .orElseThrow(() -> new NotFoundException("Disciplina não vinculada a esta turma"));
        if (date.isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("Não é possível registrar presença em data futura");
        }

        Set<Long> active = enrollments.findActiveByClass(classId).stream()
                .map(EnrolledStudent::enrollment).map(e -> e.id()).collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        for (PresenceInput in : inputs) {
            if (!seen.add(in.enrollmentId())) {
                throw new InvalidRequestException("Matrícula repetida: " + in.enrollmentId());
            }
            if (!active.contains(in.enrollmentId())) {
                throw new InvalidRequestException("Matrícula não está ativa nesta turma: " + in.enrollmentId());
            }
        }

        Map<Long, Long> existing = attendance.findByLesson(link.id(), date).stream()
                .collect(Collectors.toMap(AttendanceRecord::enrollmentId, AttendanceRecord::id));
        attendance.saveAll(inputs.stream()
                .map(in -> new AttendanceRecord(existing.get(in.enrollmentId()), link.id(), date, in.enrollmentId(), in.present()))
                .toList());
        return attendance.findSheet(link.id(), date, classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSheetEntry> sheet(Long classId, Long subjectId, LocalDate date, Long teacherUserId) {
        ClassSubject link = links.findByClassAndSubject(classId, subjectId)
                .filter(l -> teacherUserId == null || teacherUserId.equals(l.teacherUserId()))
                .orElseThrow(() -> new NotFoundException("Disciplina não vinculada a esta turma"));
        return attendance.findSheet(link.id(), date, classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummary> summary(Long enrollmentId) {
        enrollments.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada: " + enrollmentId));
        return attendance.findSummary(enrollmentId);
    }
}
