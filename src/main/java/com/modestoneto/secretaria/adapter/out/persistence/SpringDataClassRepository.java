package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataClassRepository extends JpaRepository<ClassJpaEntity, Long> {

    List<ClassJpaEntity> findAllByOrderBySchoolYearDescNameAsc();

    List<ClassJpaEntity> findBySchoolYearOrderByNameAsc(int schoolYear);
}
