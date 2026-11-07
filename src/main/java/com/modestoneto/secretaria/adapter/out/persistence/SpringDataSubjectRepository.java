package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataSubjectRepository extends JpaRepository<SubjectJpaEntity, Long> {

    List<SubjectJpaEntity> findAllByOrderByNameAsc();
}
