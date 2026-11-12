package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.ReportCardUseCase;
import com.modestoneto.secretaria.application.port.out.AttendanceRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ReportCardRepositoryPort;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.AttendanceSummary;
import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.GradeLine;
import com.modestoneto.secretaria.domain.model.ReportCard;
import com.modestoneto.secretaria.domain.model.ReportCardCalculator;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Student;
import com.modestoneto.secretaria.domain.model.SubjectReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportCardService implements ReportCardUseCase {

    private final StudentRepositoryPort students;
    private final EnrollmentRepositoryPort enrollments;
    private final ClassRepositoryPort classes;
    private final ReportCardRepositoryPort reportCards;
    private final AttendanceRepositoryPort attendance;
    private final Clock clock;

    public ReportCardService(StudentRepositoryPort students, EnrollmentRepositoryPort enrollments,
                             ClassRepositoryPort classes, ReportCardRepositoryPort reportCards,
                             AttendanceRepositoryPort attendance, Clock clock) {
        this.students = students;
        this.enrollments = enrollments;
        this.classes = classes;
        this.reportCards = reportCards;
        this.attendance = attendance;
        this.clock = clock;
    }

    @Override
    public ReportCard get(Long studentId, Integer schoolYear, Long teacherUserId) {
        int year = schoolYear != null ? schoolYear : LocalDate.now(clock).getYear();
        Student student = students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado: " + studentId));
        Enrollment enrollment = enrollments.findActiveByStudentAndYear(studentId, year)
                .orElseThrow(() -> new NotFoundException("Aluno sem matrícula ativa em " + year));
        // Professor de outra turma recebe o mesmo 404 de quem pede algo que não existe.
        if (teacherUserId != null && !classes.isTeacherOf(enrollment.classId(), teacherUserId)) {
            throw new NotFoundException("Aluno sem matrícula ativa em " + year);
        }
        SchoolClass schoolClass = classes.findById(enrollment.classId()).orElseThrow();

        // Três leituras agregadas (disciplinas, notas e frequência), qualquer que seja o tamanho do boletim:
        // nada de consulta por disciplina ou por avaliação (N+1).
        Map<Long, List<GradeLine>> gradesBySubject = reportCards.findGradeLines(enrollment.id()).stream()
                .collect(Collectors.groupingBy(GradeLine::classSubjectId));
        Map<Long, AttendanceSummary> frequency = attendance.findSummary(enrollment.id()).stream()
                .collect(Collectors.toMap(AttendanceSummary::classSubjectId, Function.identity()));

        List<SubjectReport> subjects = reportCards.findSubjects(enrollment.classId()).stream().map(ref -> {
            AttendanceSummary f = frequency.get(ref.classSubjectId());
            return ReportCardCalculator.subject(ref, gradesBySubject.getOrDefault(ref.classSubjectId(), List.of()),
                    f == null ? 0 : f.lessons(), f == null ? 0 : f.presences());
        }).toList();

        return new ReportCard(enrollment.id(), student.id(), student.fullName(), student.registrationNumber(),
                schoolClass.id(), schoolClass.name(), enrollment.schoolYear(), subjects, ReportCardCalculator.overall(subjects));
    }
}
