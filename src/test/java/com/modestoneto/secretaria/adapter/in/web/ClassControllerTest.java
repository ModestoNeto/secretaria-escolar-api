package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private static String marcador() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static String json(String nome, Object serie, String turno, Object ano, Object vagas) {
        return "{\"name\":%s,\"gradeLevel\":%s,\"shift\":%s,\"schoolYear\":%s,\"capacity\":%s}"
                .formatted(nome == null ? "null" : "\"" + nome + "\"", serie,
                        turno == null ? "null" : "\"" + turno + "\"", ano, vagas);
    }

    private static String valida(String nome, int ano) {
        return json(nome, 5, "MORNING", ano, 30);
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private ResultActions criar(Role role, String corpo) throws Exception {
        return mvc.perform(post("/classes").header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long criarTurma(String nome, int ano) throws Exception {
        String resposta = criar(Role.ADMIN, valida(nome, ano)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    // ---------- criar ----------

    @Test
    void criaTurmaComLocationECorpo() throws Exception {
        String nome = marcador();

        criar(Role.ADMIN, json(nome, 6, "AFTERNOON", 2090, 25))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/classes/\\d+")))
                .andExpect(jsonPath("$.name").value(nome))
                .andExpect(jsonPath("$.gradeLevel").value(6))
                .andExpect(jsonPath("$.shift").value("AFTERNOON"))
                .andExpect(jsonPath("$.schoolYear").value(2090))
                .andExpect(jsonPath("$.capacity").value(25));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"   \",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":0,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":13,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"shift\":\"NIGHT\",\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"schoolYear\":2090,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":1999,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2101,\"capacity\":30}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":0}",
            "{\"name\":\"X\",\"gradeLevel\":5,\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":61}",
            "{\"name\":\"X\",\"gradeLevel\":\"cinco\",\"shift\":\"MORNING\",\"schoolYear\":2090,\"capacity\":30}",
            "não é json"})
    void camposInvalidosDao400(String corpo) throws Exception {
        criar(Role.ADMIN, corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/classes"));
    }

    @Test
    void nomeComMaisDe60CaracteresDa400() throws Exception {
        criar(Role.ADMIN, valida("a".repeat(61), 2090)).andExpect(status().isBadRequest());
    }

    @Test
    void nomeEhAparado() throws Exception {
        String nome = marcador();

        criar(Role.ADMIN, valida("  " + nome + "  ", 2090))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(nome));
    }

    // ---------- unicidade (nome + ano) ----------

    @Test
    void mesmoNomeNoMesmoAnoDa409() throws Exception {
        String nome = marcador();
        criarTurma(nome, 2091);

        criar(Role.ADMIN, valida(nome, 2091))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Já existe turma com esse nome neste ano"))
                .andExpect(jsonPath("$.path").value("/classes"));
    }

    @Test
    void mesmoNomeEmOutroAnoEhAceito() throws Exception {
        String nome = marcador();
        criarTurma(nome, 2091);

        criar(Role.ADMIN, valida(nome, 2092)).andExpect(status().isCreated());
    }

    // ---------- consultar e listar ----------

    @Test
    void buscaPorIdEDevolve404SeNaoExiste() throws Exception {
        String nome = marcador();
        long id = criarTurma(nome, 2093);

        mvc.perform(get("/classes/" + id).header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(nome));
        mvc.perform(get("/classes/999999999").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listaFiltraPorAnoEOrdenaPorNome() throws Exception {
        long b = criarTurma("B " + marcador(), 2094);
        long a = criarTurma("A " + marcador(), 2094);
        criarTurma("Outro ano " + marcador(), 2095);

        String corpo = mvc.perform(get("/classes").param("schoolYear", "2094")
                        .header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Number> ids = JsonPath.read(corpo, "$[*].id");
        assertThat(ids.stream().map(Number::longValue).toList()).containsExactly(a, b);
    }

    @Test
    void listaSemFiltroTrazOsDeTodosOsAnos() throws Exception {
        String corpo = mvc.perform(get("/classes").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> nomes = JsonPath.read(corpo, "$[*].name");
        assertThat(nomes).contains("5º A", "5º B"); // seed do V3
    }

    @Test
    void anoInvalidoNoFiltroDa400() throws Exception {
        mvc.perform(get("/classes").param("schoolYear", "abc").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    // ---------- atualizar ----------

    @Test
    void atualizaTurma() throws Exception {
        long id = criarTurma(marcador(), 2096);
        String novo = marcador();

        mvc.perform(put("/classes/" + id).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(novo, 7, "EVENING", 2096, 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(novo))
                .andExpect(jsonPath("$.gradeLevel").value(7))
                .andExpect(jsonPath("$.shift").value("EVENING"))
                .andExpect(jsonPath("$.capacity").value(20));
    }

    @Test
    void atualizarParaNomeJaExistenteNoAnoDa409() throws Exception {
        String existente = marcador();
        criarTurma(existente, 2097);
        long id = criarTurma(marcador(), 2097);

        mvc.perform(put("/classes/" + id).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(valida(existente, 2097)))
                .andExpect(status().isConflict());
    }

    @Test
    void atualizarTurmaInexistenteDa404() throws Exception {
        mvc.perform(put("/classes/999999999").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(valida(marcador(), 2098)))
                .andExpect(status().isNotFound());
    }

    // ---------- permissões ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaGerenciam(Role role) throws Exception {
        criar(role, valida(marcador(), 2099)).andExpect(status().isCreated());
        mvc.perform(get("/classes").header("Authorization", bearer(role))).andExpect(status().isOk());
    }

    @Test
    void professorSoConsultaEntaoCriarEAtualizarDao403ESemTokenDa401() throws Exception {
        long id = criarTurma(marcador(), 2099);
        criar(Role.PROFESSOR, valida(marcador(), 2099)).andExpect(status().isForbidden());
        mvc.perform(put("/classes/" + id).header("Authorization", bearer(Role.PROFESSOR))
                        .contentType(MediaType.APPLICATION_JSON).content(valida(marcador(), 2099)))
                .andExpect(status().isForbidden());
        // consulta é permitida (a posse por vínculo é testada em ClassSubjectControllerTest)
        mvc.perform(get("/classes").header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isOk());
        mvc.perform(get("/classes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/classes").contentType(MediaType.APPLICATION_JSON).content(valida("Sem token", 2099)))
                .andExpect(status().isUnauthorized());
    }
}
