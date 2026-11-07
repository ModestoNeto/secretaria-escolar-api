package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassSubjectControllerTest extends AbstractMySqlIntegrationTest {

    private static final String SENHA = "senha-forte";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    /** Professor criado só para o teste, com id e token próprios. */
    private record Prof(long id, String bearer) {
    }

    private static String marcador() {
        return "Zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private Prof novoProfessor() throws Exception {
        String email = marcador().toLowerCase() + "@teste.example";
        User u = users.save(new User(null, "Professor " + email, email,
                new BCryptPasswordEncoder().encode(SENHA), Role.PROFESSOR, true));
        String corpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Prof(u.id(), "Bearer " + JsonPath.read(corpo, "$.accessToken"));
    }

    private long idDe(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long novaTurma() throws Exception {
        String corpo = "{\"name\":\"%s\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2080,\"capacity\":30}"
                .formatted(marcador());
        return idDe(mvc.perform(post("/classes").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated()));
    }

    private long novaDisciplina() throws Exception {
        return idDe(mvc.perform(post("/subjects").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"D " + marcador() + "\"}"))
                .andExpect(status().isCreated()));
    }

    private ResultActions vincular(String authorization, long turma, Object disciplina, Object professor) throws Exception {
        return mvc.perform(post("/classes/" + turma + "/subjects").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\":%s,\"teacherUserId\":%s}".formatted(disciplina, professor)));
    }

    private ResultActions vincularComoAdmin(long turma, Object disciplina, Object professor) throws Exception {
        return vincular(bearer(Role.ADMIN), turma, disciplina, professor);
    }

    // ---------- vincular ----------

    @Test
    void vinculaProfessorValido() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        Prof prof = novoProfessor();

        vincularComoAdmin(turma, disciplina, prof.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classId").value(turma))
                .andExpect(jsonPath("$.subjectId").value(disciplina))
                .andExpect(jsonPath("$.teacherUserId").value(prof.id()));
    }

    @Test
    void usuarioQueNaoEProfessorDa400() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        TestAuth.tokenDe(mvc, users, Role.ADMIN);
        TestAuth.tokenDe(mvc, users, Role.SECRETARIA);
        long admin = users.findByEmail("admin@teste.example").orElseThrow().id();
        long secretaria = users.findByEmail("secretaria@teste.example").orElseThrow().id();

        vincularComoAdmin(turma, disciplina, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuário não é professor"));
        vincularComoAdmin(turma, disciplina, secretaria).andExpect(status().isBadRequest());
    }

    @Test
    void professorInativoDa400() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        String email = marcador().toLowerCase() + "@teste.example";
        User inativo = users.save(new User(null, "Inativo", email, "x", Role.PROFESSOR, false));

        vincularComoAdmin(turma, disciplina, inativo.id())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Professor inativo"));
    }

    @Test
    void usuarioInexistenteDa400() throws Exception {
        vincularComoAdmin(novaTurma(), novaDisciplina(), 999999999L).andExpect(status().isBadRequest());
    }

    @Test
    void turmaOuDisciplinaInexistenteDa404() throws Exception {
        Prof prof = novoProfessor();

        vincularComoAdmin(999999999L, novaDisciplina(), prof.id()).andExpect(status().isNotFound());
        vincularComoAdmin(novaTurma(), 999999999L, prof.id()).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"subjectId\":1}", "{\"teacherUserId\":1}", "{\"subjectId\":\"a\",\"teacherUserId\":1}"})
    void corpoInvalidoDa400(String corpo) throws Exception {
        mvc.perform(post("/classes/" + novaTurma() + "/subjects").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mesmaDisciplinaNaMesmaTurmaDa409() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        vincularComoAdmin(turma, disciplina, novoProfessor().id()).andExpect(status().isCreated());

        vincularComoAdmin(turma, disciplina, novoProfessor().id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Disciplina já vinculada a esta turma"));
    }

    @Test
    void mesmaDisciplinaEmOutraTurmaEhAceita() throws Exception {
        long disciplina = novaDisciplina();
        Prof prof = novoProfessor();
        vincularComoAdmin(novaTurma(), disciplina, prof.id()).andExpect(status().isCreated());

        vincularComoAdmin(novaTurma(), disciplina, prof.id()).andExpect(status().isCreated());
    }

    // ---------- listar e desvincular ----------

    @Test
    void listaOsVinculosDaTurma() throws Exception {
        long turma = novaTurma();
        long d1 = novaDisciplina();
        long d2 = novaDisciplina();
        Prof a = novoProfessor();
        Prof b = novoProfessor();
        vincularComoAdmin(turma, d1, a.id()).andExpect(status().isCreated());
        vincularComoAdmin(turma, d2, b.id()).andExpect(status().isCreated());

        String corpo = mvc.perform(get("/classes/" + turma + "/subjects").header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Number> professores = JsonPath.read(corpo, "$[*].teacherUserId");
        assertThat(professores.stream().map(Number::longValue).toList()).containsExactlyInAnyOrder(a.id(), b.id());
    }

    @Test
    void listarVinculosDeTurmaInexistenteDa404() throws Exception {
        mvc.perform(get("/classes/999999999/subjects").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void desvinculaEDepoisDa404() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        vincularComoAdmin(turma, disciplina, novoProfessor().id()).andExpect(status().isCreated());

        mvc.perform(delete("/classes/" + turma + "/subjects/" + disciplina).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/classes/" + turma + "/subjects/" + disciplina).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
        // a disciplina pode ser vinculada de novo depois de desvinculada
        vincularComoAdmin(turma, disciplina, novoProfessor().id()).andExpect(status().isCreated());
    }

    // ---------- posse do professor ----------

    @Test
    void professorVeSoAsTurmasEmQueTemVinculo() throws Exception {
        long minha = novaTurma();
        long dele = novaTurma();
        Prof eu = novoProfessor();
        Prof outro = novoProfessor();
        vincularComoAdmin(minha, novaDisciplina(), eu.id()).andExpect(status().isCreated());
        vincularComoAdmin(dele, novaDisciplina(), outro.id()).andExpect(status().isCreated());

        mvc.perform(get("/classes/" + minha).header("Authorization", eu.bearer()))
                .andExpect(status().isOk());
        // 404, e não 403: não revela que a turma existe
        mvc.perform(get("/classes/" + dele).header("Authorization", eu.bearer()))
                .andExpect(status().isNotFound());

        String lista = mvc.perform(get("/classes").header("Authorization", eu.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(lista, "$[*].id");
        assertThat(ids.stream().map(Number::longValue).toList()).containsExactly(minha);
    }

    @Test
    void professorComVariasDisciplinasNaMesmaTurmaVeATurmaUmaVez() throws Exception {
        long turma = novaTurma();
        Prof eu = novoProfessor();
        vincularComoAdmin(turma, novaDisciplina(), eu.id()).andExpect(status().isCreated());
        vincularComoAdmin(turma, novaDisciplina(), eu.id()).andExpect(status().isCreated());

        mvc.perform(get("/classes").header("Authorization", eu.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void professorSemVinculoRecebeListaVazia() throws Exception {
        mvc.perform(get("/classes").header("Authorization", novoProfessor().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void filtroDeAnoValeParaOProfessor() throws Exception {
        long turma = novaTurma(); // ano 2080
        Prof eu = novoProfessor();
        vincularComoAdmin(turma, novaDisciplina(), eu.id()).andExpect(status().isCreated());

        mvc.perform(get("/classes").param("schoolYear", "2080").header("Authorization", eu.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/classes").param("schoolYear", "2081").header("Authorization", eu.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void professorVeSoOsVinculosDeleNaPropriaTurma() throws Exception {
        long turma = novaTurma();
        long minhaDisciplina = novaDisciplina();
        Prof eu = novoProfessor();
        Prof outro = novoProfessor();
        vincularComoAdmin(turma, minhaDisciplina, eu.id()).andExpect(status().isCreated());
        vincularComoAdmin(turma, novaDisciplina(), outro.id()).andExpect(status().isCreated());

        mvc.perform(get("/classes/" + turma + "/subjects").header("Authorization", eu.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subjectId").value(minhaDisciplina));
    }

    @Test
    void professorNaoVeVinculosDeTurmaAlheia() throws Exception {
        long turma = novaTurma();
        vincularComoAdmin(turma, novaDisciplina(), novoProfessor().id()).andExpect(status().isCreated());

        mvc.perform(get("/classes/" + turma + "/subjects").header("Authorization", novoProfessor().bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void desvincularTiraOAcessoDoProfessor() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        Prof eu = novoProfessor();
        vincularComoAdmin(turma, disciplina, eu.id()).andExpect(status().isCreated());
        mvc.perform(get("/classes/" + turma).header("Authorization", eu.bearer())).andExpect(status().isOk());

        mvc.perform(delete("/classes/" + turma + "/subjects/" + disciplina).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/classes/" + turma).header("Authorization", eu.bearer())).andExpect(status().isNotFound());
    }

    // ---------- permissões de escrita ----------

    @Test
    void secretariaVinculaEProfessorNaoEscreve() throws Exception {
        long turma = novaTurma();
        long disciplina = novaDisciplina();
        Prof eu = novoProfessor();

        vincular(bearer(Role.SECRETARIA), turma, disciplina, eu.id()).andExpect(status().isCreated());

        vincular(eu.bearer(), turma, novaDisciplina(), eu.id()).andExpect(status().isForbidden());
        mvc.perform(delete("/classes/" + turma + "/subjects/" + disciplina).header("Authorization", eu.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get("/classes/1/subjects")).andExpect(status().isUnauthorized());
        mvc.perform(post("/classes/1/subjects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\":1,\"teacherUserId\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/classes/1/subjects/1")).andExpect(status().isUnauthorized());
    }
}
