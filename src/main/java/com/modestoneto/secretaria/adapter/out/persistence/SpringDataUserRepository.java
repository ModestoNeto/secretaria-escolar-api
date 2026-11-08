package com.modestoneto.secretaria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.modestoneto.secretaria.domain.model.Role;

import java.util.List;
import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {

    List<UserJpaEntity> findByRoleAndActiveTrueOrderByNameAscIdAsc(Role role);

    Optional<UserJpaEntity> findByEmail(String email);
}
