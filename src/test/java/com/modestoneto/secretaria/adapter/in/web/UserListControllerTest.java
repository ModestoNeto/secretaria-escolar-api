package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserListControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private ResultActions listar(Role quem, String query) throws Exception {
        return mvc.perform(get("/users" + query)
                .header("Authorization", "Bearer " + TestAuth.tokenDe(mvc, users, quem)));
    }

    private User criar(String nome, Role role, boolean ativo) {
        return users.save(new User(null, nome, UUID.randomUUID() + "@teste.example", "hash", role, ativo));
    }

    @Test
    void listaSoProfessoresAtivosEmOrdemDeNomeSemExporHash() throws Exception {
        String m = UUID.randomUUID().toString().substring(0, 8);
        criar("Zeca " + m, Role.PROFESSOR, true);
        criar("Ana " + m, Role.PROFESSOR, true);
        criar("Inativo " + m, Role.PROFESSOR, false);
        criar("Secretario " + m, Role.SECRETARIA, true);

        String corpo = listar(Role.SECRETARIA, "?role=PROFESSOR").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        List<String> nomes = JsonPath.read(corpo, "$[*].name");
        assertThat(nomes).filteredOn(n -> n.endsWith(m)).containsExactly("Ana " + m, "Zeca " + m);
        List<String> papeis = JsonPath.read(corpo, "$[*].role");
        assertThat(papeis).containsOnly("PROFESSOR");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaPodemListar(Role quem) throws Exception {
        listar(quem, "?role=PROFESSOR").andExpect(status().isOk());
    }

    @Test
    void professorRecebe403() throws Exception {
        listar(Role.PROFESSOR, "?role=PROFESSOR").andExpect(status().isForbidden());
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(get("/users?role=PROFESSOR")).andExpect(status().isUnauthorized());
    }

    @Test
    void semRoleOuComRoleInvalidoRecebe400() throws Exception {
        listar(Role.ADMIN, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório: role"));
        listar(Role.ADMIN, "?role=DIRETOR").andExpect(status().isBadRequest());
    }
}
