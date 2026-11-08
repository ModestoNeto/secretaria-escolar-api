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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O banco é compartilhado (seed do V2 e alunos de outros testes): cada teste cria alunos com um
 * marcador único no nome e busca por ele, sem nunca afirmar o total absoluto da tabela.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentSearchTest extends AbstractMySqlIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepositoryPort users;

    @Autowired
    JdbcTemplate jdbc;

    private static String marcador() {
        return "Zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String bearer() throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, Role.ADMIN);
    }

    private long criar(String nome) throws Exception {
        String corpo = "{\"fullName\":\"%s\",\"birthDate\":\"2010-10-10\",\"guardianName\":\"Responsavel\",\"guardianPhone\":\"11999998888\",\"cpf\":\"%s\"}"
                .formatted(nome, TestCpf.novo());
        String resposta = mvc.perform(post("/students").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    /** Chama GET /students com os parâmetros (nome, valor, nome, valor...), sem problema de codificação. */
    private String buscar(String... params) throws Exception {
        var req = get("/students").header("Authorization", bearer());
        for (int i = 0; i < params.length; i += 2) {
            req = req.param(params[i], params[i + 1]);
        }
        return mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static int total(String corpo) {
        return ((Number) JsonPath.read(corpo, "$.totalElements")).intValue();
    }

    private static List<Long> ids(String corpo) {
        List<Number> ids = JsonPath.read(corpo, "$.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    // ---------- hipótese 1: a colação ai_ci ignora acento e caixa ----------

    @Test
    void ignoraAcentoECaixa() throws Exception {
        String m = marcador();
        long id = criar("José Xyz " + m);

        for (String q : List.of("jose xyz " + m, "JOSÉ XYZ " + m.toUpperCase(), "Jose Xyz " + m, "josé xyz " + m)) {
            String corpo = buscar("q", q);
            assertThat(ids(corpo)).as("q=%s", q).containsExactly(id);
        }
    }

    @Test
    void acentoNoCadastroEBuscaSemAcento() throws Exception {
        String m = marcador();
        long id = criar("João Açaí " + m);

        assertThat(ids(buscar("q", "joao acai " + m))).containsExactly(id);
        assertThat(ids(buscar("q", "JOÃO AÇAÍ " + m))).containsExactly(id);
    }

    @Test
    void buscaPorTrechoDoMeioDoNome() throws Exception {
        String m = marcador();
        long id = criar("Maria da Silva " + m);

        assertThat(ids(buscar("q", "da silva " + m))).containsExactly(id);
        assertThat(ids(buscar("q", m))).containsExactly(id);
    }

    // ---------- hipótese 2: % e _ são literais, não curingas ----------

    @Test
    void porcentagemEhLiteral() throws Exception {
        String m = marcador();
        long comPorcento = criar("100% " + m);
        criar("100x " + m); // com curinga, "100% m" também casaria com este

        assertThat(ids(buscar("q", "100% " + m))).containsExactly(comPorcento);
        // q="%" procura só quem tem % no nome, não a tabela inteira
        int comLiteral = jdbc.queryForObject(
                "select count(*) from students where instr(full_name, '%') > 0 and active = true", Integer.class);
        assertThat(total(buscar("q", "%"))).isEqualTo(comLiteral);
    }

    @Test
    void underscoreEhLiteral() throws Exception {
        String m = marcador();
        long comUnderscore = criar("A_B " + m);
        criar("AxB " + m);

        assertThat(ids(buscar("q", "A_B " + m))).containsExactly(comUnderscore);
        int comLiteral = jdbc.queryForObject(
                "select count(*) from students where instr(full_name, '_') > 0 and active = true", Integer.class);
        assertThat(total(buscar("q", "_"))).isEqualTo(comLiteral);
    }

    // ---------- hipótese 3: a contagem bate com a lista filtrada ----------

    @Test
    void totalEPaginasBatemComOFiltro() throws Exception {
        String m = marcador();
        for (int i = 0; i < 3; i++) {
            criar("Contagem " + i + " " + m);
        }

        String pagina0 = buscar("q", m, "size", "2", "page", "0");
        String pagina1 = buscar("q", m, "size", "2", "page", "1");

        assertThat(total(pagina0)).isEqualTo(3);
        assertThat((int) JsonPath.read(pagina0, "$.totalPages")).isEqualTo(2);
        assertThat(ids(pagina0)).hasSize(2);
        assertThat(ids(pagina1)).hasSize(1);
        assertThat(ids(pagina0)).doesNotContainAnyElementsOf(ids(pagina1));
    }

    @Test
    void semResultadoDevolveListaVazia() throws Exception {
        String corpo = buscar("q", marcador());

        assertThat(total(corpo)).isZero();
        assertThat(ids(corpo)).isEmpty();
        assertThat((int) JsonPath.read(corpo, "$.totalPages")).isZero();
    }

    // ---------- demais comportamentos ----------

    @Test
    void espacosNasPontasSaoIgnorados() throws Exception {
        String m = marcador();
        long id = criar("Espaco " + m);

        assertThat(ids(buscar("q", "   espaco " + m + "   "))).containsExactly(id);
    }

    @Test
    void buscaVaziaDevolveTodos() throws Exception {
        assertThat(total(buscar("q", ""))).isGreaterThanOrEqualTo(12);
        assertThat(total(buscar("q", "   "))).isEqualTo(total(buscar()));
    }

    @Test
    void combinaComOrdenacao() throws Exception {
        String m = marcador();
        long b = criar("B " + m);
        long a = criar("A " + m);

        assertThat(ids(buscar("q", m, "sort", "fullName,asc"))).containsExactly(a, b);
        assertThat(ids(buscar("q", m, "sort", "fullName,desc"))).containsExactly(b, a);
    }

    @Test
    void desativadoSoApareceComIncludeInactive() throws Exception {
        String m = marcador();
        long id = criar("Inativo " + m);
        mvc.perform(delete("/students/" + id).header("Authorization", bearer())).andExpect(status().isNoContent());

        assertThat(ids(buscar("q", m))).isEmpty();
        assertThat(ids(buscar("q", m, "includeInactive", "true"))).containsExactly(id);
    }

    @Test
    void limiteDoTamanhoDaBusca() throws Exception {
        mvc.perform(get("/students").param("q", "a".repeat(100)).header("Authorization", bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/students").param("q", "a".repeat(101)).header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("q deve ter no máximo 100 caracteres"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"' OR '1'='1", "\\", "'; DROP TABLE students; --", "%%%"})
    void entradasEstranhasNaoQuebram(String q) throws Exception {
        mvc.perform(get("/students").param("q", q).header("Authorization", bearer()))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from students", Integer.class)).isGreaterThanOrEqualTo(12);
    }
}
