package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.ClassSubject;

record ClassSubjectResponse(Long id, Long classId, Long subjectId, String subjectName, Long teacherUserId) {

    static ClassSubjectResponse from(ClassSubject cs, String subjectName) {
        return new ClassSubjectResponse(cs.id(), cs.classId(), cs.subjectId(), subjectName, cs.teacherUserId());
    }
}
