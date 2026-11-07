package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.SubjectUseCase;
import com.modestoneto.secretaria.application.port.out.SubjectRepositoryPort;
import com.modestoneto.secretaria.domain.model.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubjectService implements SubjectUseCase {

    private final SubjectRepositoryPort subjects;

    public SubjectService(SubjectRepositoryPort subjects) {
        this.subjects = subjects;
    }

    @Override
    public Subject create(String name) {
        return subjects.save(new Subject(null, name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> list() {
        return subjects.findAllByName();
    }
}
