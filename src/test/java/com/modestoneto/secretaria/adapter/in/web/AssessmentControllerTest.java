package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.SubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Shift;
import com.modestoneto.secretaria.domain.model.Student;
import com.modestoneto.secretaria.domain.model.Subject;
import com.modestoneto.secretaria.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentControllerTest extends AbstractMySqlIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepositoryPort users;
    @Autowired StudentRepositoryPort students;
    @Autowired ClassRepositoryPort classes;
    @Autowired SubjectRepositoryPort subjects;
    @Autowired ClassSubjectRepositoryPort links;
    @Autowired EnrollmentRepositoryPort enrollments;

    private long classId;
    private long subjectId;
    private long titularId;
    private long enrollA;
    private long enrollB;

    @BeforeEach
    void cenario() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        classId = classes.save(new SchoolClass(null, "T" + sufixo, 5, Shift.MORNING, 2026, 30)).id();
        subjectId = subjects.save(new Subject(null, "Disc " + sufixo)).id();
        TestAuth.tokenDe(mvc, users, Role.PROFESSOR); // garante o professor padrão de teste
        titularId = users.findByEmail("professor@teste.example").orElseThrow().id();
        links.save(new ClassSubject(null, classId, subjectId, titularId));
        enrollA = matricular("Ana " + sufixo);
        enrollB = matricular("Bruno " + sufixo);
    }

    private long matricular(String nome) {
        long aluno = students.save(new Student(null, null, nome, LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, true, TestCpf.novo())).id();
        return enrollments.save(new Enrollment(null, aluno, classId, 2026, EnrollmentStatus.ACTIVE, Instant.now())).id();
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    /** Professor que não é o titular desta disciplina. */
    private String bearerOutroProfessor() throws Exception {
        String email = "outro-" + UUID.randomUUID() + "@teste.example";
        users.save(new User(null, "Outro", email, new BCryptPasswordEncoder().encode("senha-forte"), Role.PROFESSOR, true));
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha-forte\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private String base() {
        return "/classes/" + classId + "/subjects/" + subjectId + "/assessments";
    }

    private ResultActions criar(String auth, String nome, int bimestre, String peso, String maximo) throws Exception {
        return mvc.perform(post(base()).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + nome + "\",\"term\":" + bimestre + ",\"weight\":" + peso + ",\"maxScore\":" + maximo + "}"));
    }

    private long avaliacao() throws Exception {
        String body = criar(bearer(Role.PROFESSOR), "Prova " + UUID.randomUUID(), 1, "2.5", "10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions lancar(String auth, long avaliacaoId, String corpo) throws Exception {
        return mvc.perform(put("/assessments/" + avaliacaoId + "/grades").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String notas(Object... pares) {
        var sb = new StringBuilder("{\"grades\":[");
        for (int i = 0; i < pares.length; i += 2) {
            sb.append(i > 0 ? "," : "").append("{\"enrollmentId\":").append(pares[i]).append(",\"score\":").append(pares[i + 1]).append("}");
        }
        return sb.append("]}").toString();
    }

    @Test
    void professorTitularCriaAvaliacao() throws Exception {
        criar(bearer(Role.PROFESSOR), "Prova 1", 2, "2.5", "10")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Prova 1"))
                .andExpect(jsonPath("$.term").value(2))
                .andExpect(jsonPath("$.weight").value(2.5))
                .andExpect(jsonPath("$.maxScore").value(10));
    }

    @Test
    void gestaoNaoCriaAvaliacao() throws Exception {
        criar(bearer(Role.ADMIN), "Prova", 1, "1", "10").andExpect(status().isForbidden());
        criar(bearer(Role.SECRETARIA), "Prova", 1, "1", "10").andExpect(status().isForbidden());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
    }

    @Test
    void professorQueNaoETitularRecebe404EmTudo() throws Exception {
        long id = avaliacao();
        String outro = bearerOutroProfessor();

        criar(outro, "Prova", 1, "1", "10").andExpect(status().isNotFound());
        mvc.perform(get(base()).header("Authorization", outro)).andExpect(status().isNotFound());
        mvc.perform(get("/assessments/" + id + "/grades").header("Authorization", outro)).andExpect(status().isNotFound());
        lancar(outro, id, notas(enrollA, 5)).andExpect(status().isNotFound());
    }

    @Test
    void disciplinaNaoVinculadaDa404() throws Exception {
        long outraDisciplina = subjects.save(new Subject(null, "Sem vínculo " + UUID.randomUUID())).id();

        mvc.perform(post("/classes/" + classId + "/subjects/" + outraDisciplina + "/assessments")
                        .header("Authorization", bearer(Role.PROFESSOR)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"term\":1,\"weight\":1,\"maxScore\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaCampos() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        criar(prof, "", 1, "1", "10").andExpect(status().isBadRequest());
        criar(prof, "P", 0, "1", "10").andExpect(status().isBadRequest());
        criar(prof, "P", 5, "1", "10").andExpect(status().isBadRequest());
        criar(prof, "P", 1, "0", "10").andExpect(status().isBadRequest());
        criar(prof, "P", 1, "1", "-1").andExpect(status().isBadRequest());
        criar(prof, "P", 1, "1.001", "10").andExpect(status().isBadRequest());
    }

    @Test
    void nomeRepetidoNoMesmoBimestreDa409MasEmOutroBimestrePode() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        criar(prof, "Prova", 1, "1", "10").andExpect(status().isCreated());

        criar(prof, "Prova", 1, "1", "10").andExpect(status().isConflict());
        criar(prof, "Prova", 2, "1", "10").andExpect(status().isCreated());
    }

    @Test
    void listaPorBimestreENome_gestaoETitularVeemOutroProfessorNao() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        criar(prof, "B", 2, "1", "10").andExpect(status().isCreated());
        criar(prof, "A", 2, "1", "10").andExpect(status().isCreated());
        criar(prof, "Z", 1, "1", "10").andExpect(status().isCreated());

        for (String quem : new String[]{prof, bearer(Role.SECRETARIA), bearer(Role.ADMIN)}) {
            mvc.perform(get(base()).header("Authorization", quem)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].name").value("Z"))
                    .andExpect(jsonPath("$[1].name").value("A"))
                    .andExpect(jsonPath("$[2].name").value("B"));
        }
    }

    @Test
    void pautaTrazAlunosSemNotaPorOrdemDeNome() throws Exception {
        long id = avaliacao();

        mvc.perform(get("/assessments/" + id + "/grades").header("Authorization", bearer(Role.PROFESSOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].enrollmentId").value(enrollA))
                .andExpect(jsonPath("$[0].studentName").value(org.hamcrest.Matchers.startsWith("Ana")))
                .andExpect(jsonPath("$[0].score").doesNotExist())
                .andExpect(jsonPath("$[1].enrollmentId").value(enrollB));
    }

    @Test
    void lancaNotasEDepoisCorrigeSemDuplicar() throws Exception {
        long id = avaliacao();
        String prof = bearer(Role.PROFESSOR);

        lancar(prof, id, notas(enrollA, 7.5, enrollB, 10)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(7.5))
                .andExpect(jsonPath("$[1].score").value(10));
        lancar(prof, id, notas(enrollA, 8.25)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].score").value(8.25))
                .andExpect(jsonPath("$[1].score").value(10));
    }

    @Test
    void gestaoConsultaMasNaoLancaNota() throws Exception {
        long id = avaliacao();

        lancar(bearer(Role.SECRETARIA), id, notas(enrollA, 5)).andExpect(status().isForbidden());
        lancar(bearer(Role.ADMIN), id, notas(enrollA, 5)).andExpect(status().isForbidden());
        mvc.perform(get("/assessments/" + id + "/grades").header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isOk());
    }

    @Test
    void notaAcimaDoMaximoOuNegativaDa400ENadaEGravado() throws Exception {
        long id = avaliacao();
        String prof = bearer(Role.PROFESSOR);

        lancar(prof, id, notas(enrollA, 5, enrollB, 10.5)).andExpect(status().isBadRequest());
        lancar(prof, id, notas(enrollA, -1)).andExpect(status().isBadRequest());

        // tudo ou nada: a nota válida (5) do primeiro pedido não pode ter sido gravada
        mvc.perform(get("/assessments/" + id + "/grades").header("Authorization", prof))
                .andExpect(jsonPath("$[0].score").doesNotExist());
    }

    @Test
    void matriculaCanceladaOuDeOutraTurmaOuRepetidaDa400() throws Exception {
        long id = avaliacao();
        String prof = bearer(Role.PROFESSOR);
        long outraTurma = classes.save(new SchoolClass(null, "O" + UUID.randomUUID().toString().substring(0, 8), 5,
                Shift.MORNING, 2026, 30)).id();
        long aluno = students.save(new Student(null, null, "Carla", LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, true, TestCpf.novo())).id();
        long deOutraTurma = enrollments.save(new Enrollment(null, aluno, outraTurma, 2026, EnrollmentStatus.ACTIVE, Instant.now())).id();
        enrollments.save(enrollments.findById(enrollB).orElseThrow().cancelled());

        lancar(prof, id, notas(deOutraTurma, 5)).andExpect(status().isBadRequest());
        lancar(prof, id, notas(enrollB, 5)).andExpect(status().isBadRequest());
        lancar(prof, id, notas(enrollA, 5, enrollA, 6)).andExpect(status().isBadRequest());
        lancar(prof, id, "{\"grades\":[]}").andExpect(status().isBadRequest());
        lancar(prof, id, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void matriculaCanceladaSaiDaPautaMasANotaFicaNoBanco() throws Exception {
        long id = avaliacao();
        String prof = bearer(Role.PROFESSOR);
        lancar(prof, id, notas(enrollA, 6, enrollB, 9)).andExpect(status().isOk());

        enrollments.save(enrollments.findById(enrollB).orElseThrow().cancelled());

        mvc.perform(get("/assessments/" + id + "/grades").header("Authorization", prof))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollmentId").value(enrollA));
    }

    @Test
    void avaliacaoInexistenteDa404() throws Exception {
        mvc.perform(get("/assessments/999999999/grades").header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoDesvinculaDisciplinaQueTemAvaliacoes() throws Exception {
        avaliacao();

        mvc.perform(delete("/classes/" + classId + "/subjects/" + subjectId).header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isConflict());
        assertThat(links.findByClassAndSubject(classId, subjectId)).isPresent();
    }
}
