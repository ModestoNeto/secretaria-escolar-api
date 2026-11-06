package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Subject;

record SubjectResponse(Long id, String name) {

    static SubjectResponse from(Subject s) {
        return new SubjectResponse(s.id(), s.name());
    }
}
