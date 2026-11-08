package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentCpfTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    @Autowired
    JdbcTemplate jdbc;

    private String bearer() throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, Role.ADMIN);
    }

    private static String corpo(String nome, String cpf) {
        return "{\"fullName\":\"%s\",\"birthDate\":\"2012-02-03\",\"guardianName\":\"Responsavel\",\"guardianPhone\":\"11999998888\",\"cpf\":%s}"
                .formatted(nome, cpf == null ? "null" : "\"" + cpf + "\"");
    }

    private ResultActions criar(String nome, String cpf) throws Exception {
        return mvc.perform(post("/students").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(corpo(nome, cpf)));
    }

    private long idDe(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String buscar(String q) throws Exception {
        return mvc.perform(get("/students").param("q", q).param("includeInactive", "true")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String corpo) {
        List<Number> ids = JsonPath.read(corpo, "$.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    // ---------- máscara na saída, dígitos no banco ----------

    @Test
    void cpfSemMascaraSaiMascaradoEFicaSoComDigitosNoBanco() throws Exception {
        String cpf = TestCpf.novo();

        ResultActions r = criar("Sem Mascara", cpf);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.cpf").value(TestCpf.mascarado(cpf)));

        assertThat(jdbc.queryForObject("select cpf from students where id = ?", String.class, idDe(r))).isEqualTo(cpf);
    }

    @Test
    void cpfMascaradoVindoDoFrontEhGravadoSemMascara() throws Exception {
        String cpf = TestCpf.novo();

        ResultActions r = criar("Com Mascara", TestCpf.mascarado(cpf));
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.cpf").value(TestCpf.mascarado(cpf)));

        assertThat(jdbc.queryForObject("select cpf from students where id = ?", String.class, idDe(r))).isEqualTo(cpf);
    }

    @Test
    void consultaPorIdTambemDevolveMascarado() throws Exception {
        String cpf = TestCpf.novo();
        long id = idDe(criar("Por Id", cpf).andExpect(status().isCreated()));

        mvc.perform(get("/students/" + id).header("Authorization", bearer()))
                .andExpect(jsonPath("$.cpf").value(TestCpf.mascarado(cpf)));
    }

    @Test
    void alunoDoSeedTemCpfMascarado() throws Exception {
        mvc.perform(get("/students/1").header("Authorization", bearer()))
                .andExpect(jsonPath("$.cpf").value("100.000.010-90"));
    }

    // ---------- validação ----------

    @ParameterizedTest
    @ValueSource(strings = {"11144477736", "111.444.777-36", "11111111111", "1114447773", "111.444.777-3x",
            "abc", "   ", "111444777355555"})
    void cpfInvalidoDa400SemRepetirOValor(String cpf) throws Exception {
        String resposta = criar("Invalido", cpf).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        if (!cpf.isBlank()) { // em branco não há valor para vazar
            assertThat((String) JsonPath.read(resposta, "$.message")).doesNotContain(cpf.trim());
        }
    }

    @Test
    void cpfAusenteOuNuloDa400() throws Exception {
        criar("Nulo", null).andExpect(status().isBadRequest());
        mvc.perform(post("/students").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ausente\",\"birthDate\":\"2012-02-03\",\"guardianName\":\"R\",\"guardianPhone\":\"11999998888\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- unicidade ----------

    @Test
    void cpfRepetidoDa409MesmoComOutraMascara() throws Exception {
        String cpf = TestCpf.novo();
        criar("Primeiro", cpf).andExpect(status().isCreated());

        criar("Segundo", cpf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
        criar("Terceiro", TestCpf.mascarado(cpf)).andExpect(status().isConflict());
    }

    @Test
    void editarParaOCpfDeOutroAlunoDa409EManterOProprioEhPermitido() throws Exception {
        String meu = TestCpf.novo();
        String dele = TestCpf.novo();
        long id = idDe(criar("Editavel", meu).andExpect(status().isCreated()));
        criar("Outro", dele).andExpect(status().isCreated());

        mvc.perform(put("/students/" + id).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Editavel", dele)))
                .andExpect(status().isConflict());
        mvc.perform(put("/students/" + id).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Editavel Novo", TestCpf.mascarado(meu))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(TestCpf.mascarado(meu)));
    }

    // ---------- busca: o CPF é a chave principal ----------

    @Test
    void buscaPeloCpfCompletoComEmSemMascara() throws Exception {
        String cpf = TestCpf.novo();
        long id = idDe(criar("Achado Por Cpf", cpf).andExpect(status().isCreated()));

        assertThat(ids(buscar(cpf))).containsExactly(id);
        assertThat(ids(buscar(TestCpf.mascarado(cpf)))).containsExactly(id);
        assertThat(ids(buscar("  " + TestCpf.mascarado(cpf) + "  "))).containsExactly(id);
    }

    @Test
    void buscaPeloComecoDoCpf() throws Exception {
        String cpf = TestCpf.novo();
        long id = idDe(criar("Achado Pelo Comeco", cpf).andExpect(status().isCreated()));

        assertThat(ids(buscar(cpf.substring(0, 6)))).contains(id);
        assertThat(ids(buscar(TestCpf.mascarado(cpf).substring(0, 7)))).contains(id); // "123.456"
    }

    @Test
    void cpfDeAlunoDesativadoSoApareceComIncludeInactive() throws Exception {
        String cpf = TestCpf.novo();
        long id = idDe(criar("Desativado Cpf", cpf).andExpect(status().isCreated()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/students/" + id)
                .header("Authorization", bearer())).andExpect(status().isNoContent());

        mvc.perform(get("/students").param("q", cpf).header("Authorization", bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(ids(buscar(cpf))).containsExactly(id);
    }

    @Test
    void cpfInexistenteNaoAchaNinguem() throws Exception {
        String corpo = buscar("000.000.000-00");

        assertThat(ids(corpo)).isEmpty();
    }

    @Test
    void buscaPorNomeContinuaFuncionando() throws Exception {
        long id = idDe(criar("Nome Sem Numeros Xq", TestCpf.novo()).andExpect(status().isCreated()));

        assertThat(ids(buscar("sem numeros xq"))).containsExactly(id);
    }

    @Test
    void textoComDigitosEPalavrasContinuaSendoNome() throws Exception {
        // "ana 123" não é só dígitos e máscara: vai para a busca por nome, sem erro
        mvc.perform(get("/students").param("q", "ana 123").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
}
