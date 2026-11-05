package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private String tokenDe(Role role) throws Exception {
        return TestAuth.tokenDe(mvc, users, role);
    }

    private Long idDoAdmin() {
        return users.findByEmail("admin@secretaria.example").orElseThrow().id();
    }

    @Test
    void adminConsultaUsuarioSemExporOHash() throws Exception {
        mvc.perform(get("/users/" + idDoAdmin()).header("Authorization", "Bearer " + tokenDe(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@secretaria.example"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"SECRETARIA", "PROFESSOR"})
    void outrosPapeisRecebem403(Role role) throws Exception {
        mvc.perform(get("/users/" + idDoAdmin()).header("Authorization", "Bearer " + tokenDe(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/users/" + idDoAdmin()));
    }

    @Test
    void semTokenRecebe401NoFormatoPadrao() throws Exception {
        mvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/users/1"));
    }

    @Test
    void tokenInvalidoRecebe401NoFormatoPadrao() throws Exception {
        mvc.perform(get("/users/1").header("Authorization", "Bearer lixo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void usuarioInexistenteDa404() throws Exception {
        mvc.perform(get("/users/999999").header("Authorization", "Bearer " + tokenDe(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void corsLiberaAOrigemDoFront() throws Exception {
        mvc.perform(options("/users/1")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void corsNegaOutraOrigem() throws Exception {
        mvc.perform(options("/users/1")
                        .header("Origin", "http://malicioso.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
