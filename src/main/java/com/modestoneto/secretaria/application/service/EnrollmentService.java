package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.EnrollmentUseCase;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.EnrolledStudent;
import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class EnrollmentService implements EnrollmentUseCase {

    private final EnrollmentRepositoryPort enrollments;
    private final ClassRepositoryPort classes;
    private final StudentRepositoryPort students;
    private final Clock clock;

    public EnrollmentService(EnrollmentRepositoryPort enrollments, ClassRepositoryPort classes,
                             StudentRepositoryPort students, Clock clock) {
        this.enrollments = enrollments;
        this.classes = classes;
        this.students = students;
        this.clock = clock;
    }

    @Override
    public Enrollment enroll(Long studentId, Long classId) {
        // A turma é travada ANTES de qualquer leitura comum: no MySQL (REPEATABLE READ) a primeira leitura
        // comum fixa o "retrato" da transação, e uma contagem feita com retrato velho não veria a matrícula
        // que a transação anterior acabou de confirmar, deixando passar uma vaga a mais.
        SchoolClass schoolClass = classes.findByIdForUpdate(classId)
                .orElseThrow(() -> new NotFoundException("Turma não encontrada: " + classId));
        Student student = students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado: " + studentId));
        if (!student.active()) {
            throw new InvalidRequestException("Aluno inativo");
        }
        if (enrollments.countActiveByClass(classId) >= schoolClass.capacity()) {
            throw new ConflictException("Turma sem vagas");
        }
        // a unicidade por aluno e ano é do banco (UNIQUE em student_id + active_year): o adaptador traduz em 409
        return enrollments.save(new Enrollment(null, studentId, classId, schoolClass.schoolYear(),
                EnrollmentStatus.ACTIVE, clock.instant()));
    }

    @Override
    public void cancel(Long enrollmentId) {
        Enrollment enrollment = enrollments.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada: " + enrollmentId));
        if (enrollment.active()) {
            enrollments.save(enrollment.cancelled());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledStudent> listActive(Long classId) {
        classes.findById(classId).orElseThrow(() -> new NotFoundException("Turma não encontrada: " + classId));
        return enrollments.findActiveByClass(classId);
    }
}
