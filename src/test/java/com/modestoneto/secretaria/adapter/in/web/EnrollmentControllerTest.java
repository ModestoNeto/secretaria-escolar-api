package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Shift;
import com.modestoneto.secretaria.domain.model.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    @Autowired
    StudentRepositoryPort students;

    @Autowired
    ClassRepositoryPort classes;

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private long aluno(boolean ativo) {
        return students.save(new Student(null, null, "Aluno " + UUID.randomUUID(), LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, ativo, TestCpf.novo())).id();
    }

    private long turma(int ano, int vagas) {
        return classes.save(new SchoolClass(null, "T" + UUID.randomUUID().toString().substring(0, 8), 5,
                Shift.MORNING, ano, vagas)).id();
    }

    private ResultActions matricular(Role quem, Object alunoId, Object turmaId) throws Exception {
        return mvc.perform(post("/enrollments").header("Authorization", bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":" + alunoId + ",\"classId\":" + turmaId + "}"));
    }

    private ResultActions matricular(long alunoId, long turmaId) throws Exception {
        return matricular(Role.SECRETARIA, alunoId, turmaId);
    }

    private long idDaMatricula(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions listar(long turmaId) throws Exception {
        return mvc.perform(get("/classes/" + turmaId + "/enrollments").header("Authorization", bearer(Role.SECRETARIA)));
    }

    @Test
    void matriculaCriaAtivaComOAnoDaTurmaEApareceNaLista() throws Exception {
        long a = aluno(true);
        long t = turma(2090, 10);

        matricular(a, t).andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(a))
                .andExpect(jsonPath("$.classId").value(t))
                .andExpect(jsonPath("$.schoolYear").value(2090))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.enrolledAt").isNotEmpty());

        listar(t).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(a));
    }

    @Test
    void mesmoAlunoNoMesmoAnoEmOutraTurmaDa409() throws Exception {
        long a = aluno(true);
        matricular(a, turma(2091, 10)).andExpect(status().isCreated());

        matricular(a, turma(2091, 10)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Aluno já matriculado neste ano letivo"));
    }

    @Test
    void mesmoAlunoEmOutroAnoPode() throws Exception {
        long a = aluno(true);
        matricular(a, turma(2092, 10)).andExpect(status().isCreated());
        matricular(a, turma(2093, 10)).andExpect(status().isCreated());
    }

    @Test
    void turmaCheiaDa409ECancelarLiberaAVaga() throws Exception {
        long t = turma(2094, 1);
        long primeiro = idDaMatricula(matricular(aluno(true), t).andExpect(status().isCreated()));
        long segundo = aluno(true);

        matricular(segundo, t).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Turma sem vagas"));

        mvc.perform(delete("/enrollments/" + primeiro).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
        matricular(segundo, t).andExpect(status().isCreated());
    }

    @Test
    void cancelarEhIdempotenteTiraDaListaEPermiteMatricularDeNovoNoAno() throws Exception {
        long a = aluno(true);
        long t = turma(2095, 10);
        long id = idDaMatricula(matricular(a, t).andExpect(status().isCreated()));

        mvc.perform(delete("/enrollments/" + id).header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/enrollments/" + id).header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isNoContent());

        listar(t).andExpect(jsonPath("$.length()").value(0));
        // o ano deixou de estar ocupado: o aluno pode ser matriculado outra vez (inclusive em outra turma)
        matricular(a, turma(2095, 10)).andExpect(status().isCreated());
    }

    @Test
    void cancelarMatriculaInexistenteDa404() throws Exception {
        mvc.perform(delete("/enrollments/999999999").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoInativoDa400() throws Exception {
        matricular(aluno(false), turma(2096, 10)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Aluno inativo"));
    }

    @Test
    void alunoOuTurmaInexistenteDa404() throws Exception {
        matricular(999999999L, turma(2097, 10)).andExpect(status().isNotFound());
        matricular(aluno(true), 999999999L).andExpect(status().isNotFound());
        listar(999999999L).andExpect(status().isNotFound());
    }

    @Test
    void corpoSemCamposDa400() throws Exception {
        mvc.perform(post("/enrollments").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaPodem(Role quem) throws Exception {
        long t = turma(2098, 10);
        long id = idDaMatricula(matricular(quem, aluno(true), t).andExpect(status().isCreated()));
        mvc.perform(get("/classes/" + t + "/enrollments").header("Authorization", bearer(quem)))
                .andExpect(status().isOk());
        mvc.perform(delete("/enrollments/" + id).header("Authorization", bearer(quem)))
                .andExpect(status().isNoContent());
    }

    @Test
    void professorRecebe403ESemTokenRecebe401() throws Exception {
        long t = turma(2099, 10);
        long id = idDaMatricula(matricular(aluno(true), t).andExpect(status().isCreated()));

        matricular(Role.PROFESSOR, aluno(true), t).andExpect(status().isForbidden());
        mvc.perform(delete("/enrollments/" + id).header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/classes/" + t + "/enrollments").header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/classes/" + t + "/enrollments")).andExpect(status().isUnauthorized());
    }

    /** Dispara todas as chamadas ao mesmo tempo e devolve o status HTTP de cada uma. */
    private List<Integer> emParalelo(List<long[]> pares) throws Exception {
        String token = bearer(Role.SECRETARIA);
        ExecutorService pool = Executors.newFixedThreadPool(pares.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (long[] p : pares) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return mvc.perform(post("/enrollments").header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"studentId\":" + p[0] + ",\"classId\":" + p[1] + "}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                codigos.add(f.get());
            }
            return codigos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ultimaVagaDisputadaPorVariosAlunosFicaComExatamenteUm() throws Exception {
        long t = turma(2089, 3);
        List<long[]> pares = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            pares.add(new long[]{aluno(true), t});
        }

        List<Integer> codigos = emParalelo(pares);

        assertThat(codigos.stream().filter(c -> c == 201).count()).isEqualTo(3);
        assertThat(codigos.stream().filter(c -> c == 409).count()).isEqualTo(9);
        listar(t).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void oMesmoAlunoEmDuasTurmasDoMesmoAnoAoMesmoTempoFicaEmUma() throws Exception {
        long a = aluno(true);
        long t1 = turma(2088, 10);
        long t2 = turma(2088, 10);

        List<Integer> codigos = emParalelo(List.of(new long[]{a, t1}, new long[]{a, t2}));

        assertThat(codigos).containsExactlyInAnyOrder(201, 409);
    }
}
