package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.TestCpf;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentPaginationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private String listar(String query) throws Exception {
        return mvc.perform(get("/students" + query).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long criar(String nome, String nascimento) throws Exception {
        String corpo = "{\"fullName\":\"%s\",\"birthDate\":\"%s\",\"guardianName\":\"Responsavel\",\"guardianPhone\":\"11999998888\",\"cpf\":\"%s\"}"
                .formatted(nome, nascimento, TestCpf.novo());
        String resposta = mvc.perform(post("/students").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    @Test
    void metadadosDaPaginaBatemComOTotal() throws Exception {
        String corpo = listar("?size=5");
        int total = ((Number) JsonPath.read(corpo, "$.totalElements")).intValue();

        assertThat((List<?>) JsonPath.read(corpo, "$.content")).hasSize(Math.min(5, total));
        assertThat((int) JsonPath.read(corpo, "$.page")).isZero();
        assertThat((int) JsonPath.read(corpo, "$.size")).isEqualTo(5);
        assertThat((int) JsonPath.read(corpo, "$.totalPages")).isEqualTo((int) Math.ceil(total / 5.0));
        assertThat(total).isGreaterThanOrEqualTo(12); // o seed do V2
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "desc"})
    void ordenaPorMatriculaNasDuasDirecoes(String direcao) throws Exception {
        List<String> matriculas = JsonPath.read(listar("?size=100&sort=registrationNumber," + direcao),
                "$.content[*].registrationNumber");

        assertThat(matriculas).hasSizeGreaterThanOrEqualTo(12)
                .isSortedAccordingTo(direcao.equals("asc")
                        ? java.util.Comparator.<String>naturalOrder()
                        : java.util.Comparator.<String>reverseOrder());
    }

    @Test
    void desempatePorIdMantemAOrdemEstavelEntrePaginas() throws Exception {
        // vários alunos com a mesma data de nascimento: sem desempate a ordem entre páginas seria arbitrária
        for (int i = 0; i < 5; i++) {
            criar("Mesmo Nascimento " + i, "2001-01-01");
        }
        int total = ((Number) JsonPath.read(listar("?size=1"), "$.totalElements")).intValue();

        List<Long> percorridos = new ArrayList<>();
        for (int pagina = 0; pagina * 3 < total; pagina++) {
            List<Number> ids = JsonPath.read(listar("?size=3&sort=birthDate,asc&page=" + pagina), "$.content[*].id");
            ids.forEach(n -> percorridos.add(n.longValue()));
        }

        assertThat(percorridos).hasSize(total).doesNotHaveDuplicates();
        List<Number> iguais = JsonPath.read(listar("?size=100&sort=birthDate,asc"),
                "$.content[?(@.birthDate=='2001-01-01')].id");
        assertThat(iguais.stream().map(Number::longValue).toList()).isSorted();
    }

    @Test
    void desativadoSoApareceComIncludeInactive() throws Exception {
        long id = criar("Vai Ser Desativado", "2002-02-02");
        mvc.perform(delete("/students/" + id).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());

        // a matrícula mais recente vem primeiro na ordem decrescente
        List<Number> padrao = JsonPath.read(listar("?size=100&sort=registrationNumber,desc"), "$.content[*].id");
        List<Number> todos = JsonPath.read(listar("?size=100&sort=registrationNumber,desc&includeInactive=true"),
                "$.content[*].id");

        assertThat(new HashSet<>(padrao.stream().map(Number::longValue).toList())).doesNotContain(id);
        assertThat(new HashSet<>(todos.stream().map(Number::longValue).toList())).contains(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?sort=password",
            "?sort=id",
            "?sort=fullName,sideways",
            "?sort=fullName;drop",
            "?size=101",
            "?size=0",
            "?page=-1",
            "?page=abc"})
    void parametrosInvalidosDao400(String query) throws Exception {
        mvc.perform(get("/students" + query).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/students"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaListam(Role role) throws Exception {
        mvc.perform(get("/students").header("Authorization", bearer(role)))
                .andExpect(status().isOk());
    }

    @Test
    void professorRecebe403ESemTokenRecebe401() throws Exception {
        mvc.perform(get("/students").header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/students")).andExpect(status().isUnauthorized());
    }
}
