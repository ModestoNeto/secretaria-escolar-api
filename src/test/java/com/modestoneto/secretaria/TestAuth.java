package com.modestoneto.secretaria;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cria (se preciso) um usuário do papel e devolve o token de acesso dele. */
public final class TestAuth {

    private static final String SENHA = "senha-forte";

    private TestAuth() {
    }

    public static String tokenDe(MockMvc mvc, UserRepositoryPort users, Role role) throws Exception {
        String email = role.name().toLowerCase() + "@teste.example";
        if (users.findByEmail(email).isEmpty()) {
            users.save(new User(null, role.name(), email, new BCryptPasswordEncoder().encode(SENHA), role, true));
        }
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
