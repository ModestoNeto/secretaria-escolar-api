package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.domain.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StudentPersistenceAdapter.class, StudentPersistenceAdapterTest.RelogioFixo.class})
class StudentPersistenceAdapterTest extends AbstractMySqlIntegrationTest {

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2031-05-10T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    StudentPersistenceAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    private Student novo() {
        return new Student(null, null, "Aluno Adaptador", LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, true, TestCpf.novo());
    }

    @Test
    void matriculaUsaOAnoDoRelogioEOIdComSeisDigitos() {
        Student salvo = adapter.save(novo());

        assertThat(salvo.registrationNumber()).isEqualTo("2031%06d".formatted(salvo.id()));
        // lido do banco, não do objeto devolvido: o UPDATE da matrícula realmente aconteceu
        assertThat(jdbc.queryForObject("select registration_number from students where id = ?",
                String.class, salvo.id())).isEqualTo(salvo.registrationNumber());
    }

    @Test
    void atualizarNaoMudaAMatricula() {
        Student salvo = adapter.save(novo());

        Student editado = adapter.save(new Student(salvo.id(), salvo.registrationNumber(), "Outro Nome",
                salvo.birthDate(), salvo.guardianName(), salvo.guardianPhone(), "a@b.test", false, salvo.cpf()));

        assertThat(editado.registrationNumber()).isEqualTo(salvo.registrationNumber());
        assertThat(adapter.findById(salvo.id())).get()
                .satisfies(s -> {
                    assertThat(s.fullName()).isEqualTo("Outro Nome");
                    assertThat(s.active()).isFalse();
                });
    }

    @Test
    void idInexistenteNaoEncontra() {
        assertThat(adapter.findById(-1L)).isEmpty();
    }
}
