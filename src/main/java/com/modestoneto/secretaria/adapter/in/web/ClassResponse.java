package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Shift;

record ClassResponse(Long id, String name, int gradeLevel, Shift shift, int schoolYear, int capacity) {

    static ClassResponse from(SchoolClass c) {
        return new ClassResponse(c.id(), c.name(), c.gradeLevel(), c.shift(), c.schoolYear(), c.capacity());
    }
}
