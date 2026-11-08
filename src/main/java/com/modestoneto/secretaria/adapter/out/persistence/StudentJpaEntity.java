package com.modestoneto.secretaria.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "students")
class StudentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "guardian_name", nullable = false, length = 160)
    private String guardianName;

    @Column(name = "guardian_phone", nullable = false, length = 11)
    private String guardianPhone;

    @Column(length = 160)
    private String email;

    @Column(nullable = false)
    private boolean active;

    // só os 11 dígitos, sem máscara; nulo nos alunos de antes do CPF existir
    @Column(length = 11)
    private String cpf;

    protected StudentJpaEntity() {
    }

    StudentJpaEntity(Long id, String registrationNumber, String fullName, LocalDate birthDate,
                     String guardianName, String guardianPhone, String email, boolean active, String cpf) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.email = email;
        this.active = active;
        this.cpf = cpf;
    }

    Long getId() { return id; }
    String getRegistrationNumber() { return registrationNumber; }
    void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    String getFullName() { return fullName; }
    LocalDate getBirthDate() { return birthDate; }
    String getGuardianName() { return guardianName; }
    String getGuardianPhone() { return guardianPhone; }
    String getEmail() { return email; }
    boolean isActive() { return active; }
    String getCpf() { return cpf; }
}
