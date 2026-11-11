package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Assessment;

import java.math.BigDecimal;

record AssessmentResponse(Long id, String name, int term, BigDecimal weight, BigDecimal maxScore) {

    static AssessmentResponse from(Assessment a) {
        return new AssessmentResponse(a.id(), a.name(), a.term(), a.weight(), a.maxScore());
    }
}
