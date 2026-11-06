package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.Shift;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_classes")
class ClassJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Shift shift;

    @Column(name = "school_year", nullable = false)
    private int schoolYear;

    @Column(nullable = false)
    private int capacity;

    protected ClassJpaEntity() {
    }

    ClassJpaEntity(Long id, String name, int gradeLevel, Shift shift, int schoolYear, int capacity) {
        this.id = id;
        this.name = name;
        this.gradeLevel = gradeLevel;
        this.shift = shift;
        this.schoolYear = schoolYear;
        this.capacity = capacity;
    }

    Long getId() { return id; }
    String getName() { return name; }
    int getGradeLevel() { return gradeLevel; }
    Shift getShift() { return shift; }
    int getSchoolYear() { return schoolYear; }
    int getCapacity() { return capacity; }
}
