package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.domain.exception.ConflictException;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest extends AbstractMySqlIntegrationTest {

    @Autowired
    UserPersistenceAdapter adapter;

    private User novo(String email) {
        return new User(null, "Maria", email, "hash", Role.SECRETARIA, true);
    }

    @Test
    void salvaEBuscaPorIdEPorEmail() {
        User salvo = adapter.save(novo("maria@teste.example"));

        assertThat(salvo.id()).isNotNull();
        assertThat(adapter.findById(salvo.id())).contains(salvo);
        assertThat(adapter.findByEmail("maria@teste.example")).contains(salvo);
    }

    @Test
    void emailDuplicadoViraConflito() {
        adapter.save(novo("dup@teste.example"));

        assertThatThrownBy(() -> adapter.save(novo("dup@teste.example")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void emailIgnoraCaixa() {
        adapter.save(novo("caixa@teste.example"));

        assertThat(adapter.findByEmail("CAIXA@Teste.Example")).isPresent();
    }

    @Test
    void adminDoSeedExiste() {
        User admin = adapter.findByEmail("admin@secretaria.example").orElseThrow();

        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(admin.active()).isTrue();
    }
}
