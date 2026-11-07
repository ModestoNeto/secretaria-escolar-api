package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
class SubjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    protected SubjectJpaEntity() {
    }

    SubjectJpaEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    Long getId() { return id; }
    String getName() { return name; }
}
