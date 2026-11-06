package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataStudentRepository extends JpaRepository<StudentJpaEntity, Long> {

    // O Containing escapa % e _ do parâmetro; a colação da coluna (utf8mb4_0900_ai_ci)
    // ignora acento e caixa no LIKE, sem normalização no código.
    Page<StudentJpaEntity> findByFullNameContaining(String q, Pageable pageable);

    Page<StudentJpaEntity> findByFullNameContainingAndActiveTrue(String q, Pageable pageable);
}
