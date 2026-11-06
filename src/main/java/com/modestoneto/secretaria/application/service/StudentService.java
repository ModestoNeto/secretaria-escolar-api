package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.PageResult;
import com.modestoneto.secretaria.application.port.in.StudentUseCase;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.TreeSet;

@Service
@Transactional
public class StudentService implements StudentUseCase {

    private static final int MAX_QUERY_LENGTH = 100;

    private final StudentRepositoryPort students;

    public StudentService(StudentRepositoryPort students) {
        this.students = students;
    }

    @Override
    public Student create(StudentData d) {
        return students.save(new Student(null, null, d.fullName(), d.birthDate(),
                d.guardianName(), d.guardianPhone(), d.email(), true));
    }

    @Override
    public Student update(Long id, StudentData d) {
        Student atual = findById(id);
        // a matrícula e o estado ativo nunca vêm do cliente
        return students.save(new Student(atual.id(), atual.registrationNumber(), d.fullName(),
                d.birthDate(), d.guardianName(), d.guardianPhone(), d.email(), atual.active()));
    }

    @Override
    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return students.findById(id)
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado: " + id));
    }

    @Override
    public void deactivate(Long id) {
        students.save(findById(id).deactivated());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Student> search(String q, boolean includeInactive, PageRequest pageRequest) {
        if (!SORT_FIELDS.contains(pageRequest.sort())) {
            throw new InvalidRequestException("sort deve ser um de: " + String.join(", ", new TreeSet<>(SORT_FIELDS)));
        }
        String termo = q == null ? "" : q.trim();
        if (termo.length() > MAX_QUERY_LENGTH) {
            throw new InvalidRequestException("q deve ter no máximo " + MAX_QUERY_LENGTH + " caracteres");
        }
        return students.search(termo, includeInactive, pageRequest);
    }
}
