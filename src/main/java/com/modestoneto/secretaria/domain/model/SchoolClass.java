package com.modestoneto.secretaria.domain.model;

public record SchoolClass(Long id, String name, int gradeLevel, Shift shift, int schoolYear, int capacity) {
}
