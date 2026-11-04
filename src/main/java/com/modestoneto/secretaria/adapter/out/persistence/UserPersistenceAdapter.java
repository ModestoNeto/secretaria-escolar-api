package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        try {
            return UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("E-mail já cadastrado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserMapper::toDomain);
    }
}
