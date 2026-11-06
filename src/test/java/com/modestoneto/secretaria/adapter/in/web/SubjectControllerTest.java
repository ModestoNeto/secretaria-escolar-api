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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubjectControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private static String marcador() {
        return "Zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private ResultActions criar(Role role, String corpo) throws Exception {
        return mvc.perform(post("/subjects").header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(String nome) {
        return "{\"name\":\"%s\"}".formatted(nome);
    }

    @Test
    void criaDisciplina() throws Exception {
        String nome = "Disciplina " + marcador();

        criar(Role.ADMIN, corpo(nome))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(nome));
    }

    @Test
    void espacosNasPontasSaoAparados() throws Exception {
        String nome = "Aparada " + marcador();

        criar(Role.ADMIN, corpo("   " + nome + "  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(nome));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}", "{\"name\":\"não é json"})
    void nomeInvalidoDa400(String corpo) throws Exception {
        criar(Role.ADMIN, corpo).andExpect(status().isBadRequest());
    }

    @Test
    void nomeComMaisDe80CaracteresDa400() throws Exception {
        criar(Role.ADMIN, corpo("a".repeat(81))).andExpect(status().isBadRequest());
        criar(Role.ADMIN, corpo("b".repeat(68) + marcador())).andExpect(status().isCreated()); // 80 exatos
    }

    @Test
    void duplicadaDa409NoFormatoPadrao() throws Exception {
        String nome = "Duplicada " + marcador();
        criar(Role.ADMIN, corpo(nome)).andExpect(status().isCreated());

        criar(Role.ADMIN, corpo(nome))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Disciplina já cadastrada"))
                .andExpect(jsonPath("$.path").value("/subjects"));
    }

    @Test
    void duplicidadeIgnoraAcentoECaixa() throws Exception {
        String m = marcador();
        criar(Role.ADMIN, corpo("Árvores " + m)).andExpect(status().isCreated());

        criar(Role.ADMIN, corpo("arvores " + m.toUpperCase())).andExpect(status().isConflict());
    }

    @Test
    void listaTraDisciplinasEmOrdemDeNome() throws Exception {
        String corpoLista = mvc.perform(get("/subjects").header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> nomes = JsonPath.read(corpoLista, "$[*].name");
        // seed do V3, na ordem alfabética (outros testes inserem nomes no meio, então filtramos)
        List<String> seed = List.of("Ciências", "Geografia", "História", "Língua Portuguesa", "Matemática");
        assertThat(nomes.stream().filter(seed::contains).toList()).containsExactlyElementsOf(seed);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaCriamEListam(Role role) throws Exception {
        criar(role, corpo("Permitida " + marcador())).andExpect(status().isCreated());
        mvc.perform(get("/subjects").header("Authorization", bearer(role))).andExpect(status().isOk());
    }

    @Test
    void professorRecebe403ESemTokenRecebe401() throws Exception {
        criar(Role.PROFESSOR, corpo("Negada")).andExpect(status().isForbidden());
        mvc.perform(get("/subjects").header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/subjects")).andExpect(status().isUnauthorized());
        mvc.perform(post("/subjects").contentType(MediaType.APPLICATION_JSON).content(corpo("Sem token")))
                .andExpect(status().isUnauthorized());
    }
}
