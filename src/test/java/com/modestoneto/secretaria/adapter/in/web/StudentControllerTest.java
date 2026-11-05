package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Year;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    @Autowired
    JdbcTemplate jdbc;

    // ---------- apoio ----------

    private static String q(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    private static String json(String nome, String nascimento, String telefone, String email) {
        return "{\"fullName\":%s,\"birthDate\":%s,\"guardianName\":\"Responsável Teste\",\"guardianPhone\":%s,\"email\":%s}"
                .formatted(q(nome), q(nascimento), q(telefone), q(email));
    }

    private static String valido() {
        return json("Aluno de Teste", "2014-05-10", "11999998888", null);
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private ResultActions criar(String authorization, String corpo) throws Exception {
        var req = post("/students").contentType(MediaType.APPLICATION_JSON).content(corpo);
        return mvc.perform(authorization == null ? req : req.header("Authorization", authorization));
    }

    private long criarComoAdmin() throws Exception {
        MvcResult r = criar(bearer(Role.ADMIN), valido()).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // ---------- matriz de permissões (ADMIN e SECRETARIA sim; PROFESSOR não; sem token 401) ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SECRETARIA"})
    void adminESecretariaFazemOCicloCompleto(Role role) throws Exception {
        String auth = bearer(role);
        MvcResult criado = criar(auth, valido()).andExpect(status().isCreated()).andReturn();
        long id = ((Number) JsonPath.read(criado.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/students/" + id).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(put("/students/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nome Editado", "2014-05-10", "11999998888", null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Nome Editado"));
        mvc.perform(delete("/students/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
    }

    @Test
    void professorRecebe403EmTodasAsOperacoes() throws Exception {
        long id = criarComoAdmin();
        String auth = bearer(Role.PROFESSOR);

        criar(auth, valido()).andExpect(status().isForbidden());
        mvc.perform(get("/students/" + id).header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(put("/students/" + id).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isForbidden());
        mvc.perform(delete("/students/" + id).header("Authorization", auth)).andExpect(status().isForbidden());
    }

    @Test
    void semTokenRecebe401EmTodasAsOperacoes() throws Exception {
        criar(null, valido()).andExpect(status().isUnauthorized());
        mvc.perform(get("/students/1")).andExpect(status().isUnauthorized());
        mvc.perform(put("/students/1").contentType(MediaType.APPLICATION_JSON).content(valido()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/students/1")).andExpect(status().isUnauthorized());
    }

    // ---------- criação e matrícula ----------

    @Test
    void criacaoDevolve201ComLocationEMatriculaNoFormato() throws Exception {
        MvcResult r = criar(bearer(Role.SECRETARIA), valido())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/students/\\d+")))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String corpo = r.getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        String matricula = JsonPath.read(corpo, "$.registrationNumber");

        assertThat(matricula).matches("\\d{10}")
                .startsWith(String.valueOf(Year.now().getValue()))
                .endsWith("%06d".formatted(id));
        // a troca da matrícula provisória precisa ter chegado ao banco, não só à resposta
        assertThat(jdbc.queryForObject("select registration_number from students where id = ?",
                String.class, id)).isEqualTo(matricula);
    }

    @Test
    void duasCriacoesGeramMatriculasDiferentes() throws Exception {
        long a = criarComoAdmin();
        long b = criarComoAdmin();

        assertThat(a).isNotEqualTo(b);
        assertThat(jdbc.queryForObject("select count(distinct registration_number) from students where id in (?, ?)",
                Integer.class, a, b)).isEqualTo(2);
    }

    @Test
    void emailEmBrancoViraNulo() throws Exception {
        criar(bearer(Role.ADMIN), json("Sem Email", "2014-05-10", "11999998888", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    // ---------- validação ----------

    static Stream<String> corposInvalidos() {
        return Stream.of(
                json("   ", "2014-05-10", "11999998888", null),             // nome em branco
                json(null, "2014-05-10", "11999998888", null),              // nome ausente
                json("Fulano", null, "11999998888", null),                  // nascimento ausente
                json("Fulano", "2999-01-01", "11999998888", null),          // nascimento no futuro
                json("Fulano", "2014-05-10", "1199999", null),              // telefone curto
                json("Fulano", "2014-05-10", "11 99999-8888", null),        // telefone com máscara
                json("Fulano", "2014-05-10", "11999998888", "nao-e-email"), // e-mail inválido
                json("Fulano", "2014-05-10", "11999998888", "   "),         // e-mail só com espaços
                json("x".repeat(161), "2014-05-10", "11999998888", null),   // nome longo demais
                "{nao e json",                                              // JSON quebrado
                json("Fulano", "2014-13-45", "11999998888", null));         // data impossível
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void corpoInvalidoDa400NoFormatoPadrao(String corpo) throws Exception {
        criar(bearer(Role.ADMIN), corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/students"));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mvc.perform(get("/students/abc").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido: id"));
    }

    // ---------- leitura, edição e desativação ----------

    @Test
    void idInexistenteDa404() throws Exception {
        mvc.perform(get("/students/999999").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/students/999999").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isNotFound());
        mvc.perform(delete("/students/999999").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void edicaoMantemAMatriculaEOEstadoAtivo() throws Exception {
        String auth = bearer(Role.ADMIN);
        MvcResult r = criar(auth, valido()).andExpect(status().isCreated()).andReturn();
        String corpo = r.getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        String matricula = JsonPath.read(corpo, "$.registrationNumber");
        mvc.perform(delete("/students/" + id).header("Authorization", auth)).andExpect(status().isNoContent());

        mvc.perform(put("/students/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nome Novo", "2013-01-02", "21988887777", "novo@exemplo.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nome Novo"))
                .andExpect(jsonPath("$.email").value("novo@exemplo.test"))
                .andExpect(jsonPath("$.registrationNumber").value(matricula))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void desativarNaoApagaERepetirTambemDa204() throws Exception {
        String auth = bearer(Role.ADMIN);
        long id = criarComoAdmin();

        mvc.perform(delete("/students/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(delete("/students/" + id).header("Authorization", auth)).andExpect(status().isNoContent());

        mvc.perform(get("/students/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertThat(jdbc.queryForObject("select count(*) from students where id = ?", Integer.class, id)).isEqualTo(1);
    }

    // ---------- seed do V2 ----------

    @Test
    void seedTrazAlunosComMatriculaNoFormato() {
        assertThat(jdbc.queryForObject("select count(*) from students where full_name = 'José Carlos Lima'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from students", Integer.class)).isGreaterThanOrEqualTo(12);
        assertThat(jdbc.queryForObject(
                "select count(*) from students where registration_number not regexp '^[0-9]{10}$'",
                Integer.class)).isZero();
    }
}
