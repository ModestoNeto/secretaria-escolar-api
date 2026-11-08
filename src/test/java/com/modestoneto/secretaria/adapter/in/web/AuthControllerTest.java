package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractMySqlIntegrationTest {

    private static final String ADMIN = "admin@secretaria.example";
    private static final String ADMIN_PASSWORD = "2307"; // senha fictícia do seed (V1)

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String loginToken(String field) throws Exception {
        String body = login(ADMIN, ADMIN_PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$." + field);
    }

    private ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    @Test
    void loginDoAdminDevolveOsDoisTokens() throws Exception {
        login(ADMIN, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void senhaErradaDa401() throws Exception {
        login(ADMIN, "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void emailInexistenteDa401ComAMesmaMensagem() throws Exception {
        login("ninguem@teste.example", "2307")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void usuarioInativoDa401() throws Exception {
        users.save(new User(null, "Inativo", "inativo@teste.example",
                new BCryptPasswordEncoder().encode("senha-forte"), Role.PROFESSOR, false));

        login("inativo@teste.example", "senha-forte").andExpect(status().isUnauthorized());
    }

    @Test
    void cincoFalhasBloqueiamOEmailMesmoComSenhaCorreta() throws Exception {
        String email = "bloqueado-" + System.nanoTime() + "@teste.example";
        users.save(new User(null, "Bloqueado", email,
                new BCryptPasswordEncoder().encode("senha-forte"), Role.PROFESSOR, true));

        for (int i = 0; i < 5; i++) {
            login(email, "errada").andExpect(status().isUnauthorized());
        }

        login(email, "senha-forte")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
        // outro e-mail não é afetado
        login(ADMIN, ADMIN_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void emailInexistenteTambemEhLimitado() throws Exception {
        String email = "fantasma-" + System.nanoTime() + "@teste.example";
        for (int i = 0; i < 5; i++) {
            login(email, "x").andExpect(status().isUnauthorized());
        }
        login(email, "x").andExpect(status().isTooManyRequests());
    }

    @Test
    void loginComSucessoZeraAsFalhas() throws Exception {
        String email = "zera-" + System.nanoTime() + "@teste.example";
        users.save(new User(null, "Zera", email,
                new BCryptPasswordEncoder().encode("senha-forte"), Role.PROFESSOR, true));

        for (int i = 0; i < 4; i++) {
            login(email, "errada").andExpect(status().isUnauthorized());
        }
        login(email, "senha-forte").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(email, "errada").andExpect(status().isUnauthorized());
        }
        login(email, "senha-forte").andExpect(status().isOk());
    }

    @Test
    void corpoInvalidoDa400()throws Exception {
        login("nao-e-email", "").andExpect(status().isBadRequest());
    }

    @Test
    void refreshValidoDevolveNovoPar() throws Exception {
        refresh(loginToken("refreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void accessTokenNaoServeNoRefresh() throws Exception {
        refresh(loginToken("accessToken")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNaoServeNumaRotaProtegida() throws Exception {
        mvc.perform(get("/qualquer").header("Authorization", "Bearer " + loginToken("refreshToken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get("/qualquer")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoDa401() throws Exception {
        mvc.perform(get("/qualquer").header("Authorization", "Bearer " + loginToken("accessToken") + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutDa204() throws Exception {
        mvc.perform(post("/auth/logout")).andExpect(status().isNoContent());
    }
}
