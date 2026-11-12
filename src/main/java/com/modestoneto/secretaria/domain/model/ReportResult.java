package com.modestoneto.secretaria.domain.model;

/** Situação do aluno: IN_PROGRESS enquanto ainda não há nota lançada (e a frequência não reprova). */
public enum ReportResult {
    APPROVED, RECOVERY, FAILED, IN_PROGRESS
}
