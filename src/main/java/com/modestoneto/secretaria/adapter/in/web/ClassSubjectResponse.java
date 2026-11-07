package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.ClassSubject;

record ClassSubjectResponse(Long id, Long classId, Long subjectId, Long teacherUserId) {

    static ClassSubjectResponse from(ClassSubject cs) {
        return new ClassSubjectResponse(cs.id(), cs.classId(), cs.subjectId(), cs.teacherUserId());
    }
}
