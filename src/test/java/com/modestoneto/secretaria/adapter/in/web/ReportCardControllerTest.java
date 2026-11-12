package com.modestoneto.secretaria.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.modestoneto.secretaria.AbstractMySqlIntegrationTest;
import com.modestoneto.secretaria.TestAuth;
import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.application.port.out.AssessmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.AttendanceRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassRepositoryPort;
import com.modestoneto.secretaria.application.port.out.ClassSubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.EnrollmentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.StudentRepositoryPort;
import com.modestoneto.secretaria.application.port.out.SubjectRepositoryPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.model.Assessment;
import com.modestoneto.secretaria.domain.model.AttendanceRecord;
import com.modestoneto.secretaria.domain.model.ClassSubject;
import com.modestoneto.secretaria.domain.model.Enrollment;
import com.modestoneto.secretaria.domain.model.EnrollmentStatus;
import com.modestoneto.secretaria.domain.model.Grade;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.SchoolClass;
import com.modestoneto.secretaria.domain.model.Shift;
import com.modestoneto.secretaria.domain.model.Student;
import com.modestoneto.secretaria.domain.model.Subject;
import com.modestoneto.secretaria.domain.model.User;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// As estatísticas do Hibernate contam os comandos SQL de cada pedido (teste do N+1 no fim da classe).
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class ReportCardControllerTest extends AbstractMySqlIntegrationTest {

    private static final int ANO = LocalDate.now().getYear();

    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory emf;
    @Autowired UserRepositoryPort users;
    @Autowired StudentRepositoryPort students;
    @Autowired ClassRepositoryPort classes;
    @Autowired SubjectRepositoryPort subjects;
    @Autowired ClassSubjectRepositoryPort links;
    @Autowired EnrollmentRepositoryPort enrollments;
    @Autowired AssessmentRepositoryPort assessments;
    @Autowired AttendanceRepositoryPort attendance;

    private String sufixo;
    private long classId;
    private long titularId;
    private long studentId;
    private long enrollmentId;

    @BeforeEach
    void cenario() throws Exception {
        sufixo = UUID.randomUUID().toString().substring(0, 8);
        classId = classes.save(new SchoolClass(null, "T" + sufixo, 5, Shift.MORNING, ANO, 30)).id();
        TestAuth.tokenDe(mvc, users, Role.PROFESSOR); // garante o professor padrão de teste
        titularId = users.findByEmail("professor@teste.example").orElseThrow().id();
        studentId = novoAluno("Ana " + sufixo);
        enrollmentId = enrollments.save(new Enrollment(null, studentId, classId, ANO, EnrollmentStatus.ACTIVE, Instant.now())).id();
    }

    private long novoAluno(String nome) {
        return students.save(new Student(null, null, nome, LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, true, TestCpf.novo())).id();
    }

    private String bearer(Role role) throws Exception {
        return "Bearer " + TestAuth.tokenDe(mvc, users, role);
    }

    private String bearerOutroProfessor() throws Exception {
        String email = "outro-" + UUID.randomUUID() + "@teste.example";
        users.save(new User(null, "Outro", email, new BCryptPasswordEncoder().encode("senha-forte"), Role.PROFESSOR, true));
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha-forte\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    /** Vincula uma disciplina nova à turma, com o professor padrão como titular, e devolve o id do vínculo. */
    private long disciplina(String nome) {
        long subject = subjects.save(new Subject(null, nome + " " + sufixo)).id();
        return links.save(new ClassSubject(null, classId, subject, titularId)).id();
    }

    private void nota(long vinculo, int bimestre, String peso, String maximo, String nota) {
        long avaliacao = assessments.save(new Assessment(null, vinculo, "A" + UUID.randomUUID().toString().substring(0, 8),
                bimestre, new BigDecimal(peso), new BigDecimal(maximo))).id();
        assessments.saveGrades(List.of(new Grade(null, avaliacao, enrollmentId, new BigDecimal(nota))));
    }

    /** Registra aulas em dias seguidos; as primeiras "presencas" aulas com presença e o resto com falta. */
    private void aulas(long vinculo, int total, int presencas) {
        List<AttendanceRecord> registros = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            registros.add(new AttendanceRecord(null, vinculo, LocalDate.of(2026, 3, 1).plusDays(i), enrollmentId, i < presencas));
        }
        attendance.saveAll(registros);
    }

    private ResultActions boletim(String auth, long aluno) throws Exception {
        return mvc.perform(get("/students/" + aluno + "/report-card").header("Authorization", auth));
    }

    // ---------- conteúdo ----------

    @Test
    void boletimTrazMediasFrequenciaEResultadoPorDisciplina() throws Exception {
        long mat = disciplina("Matemática");
        nota(mat, 1, "2", "10", "8");
        nota(mat, 2, "1", "5", "5");
        aulas(mat, 4, 4);
        long por = disciplina("Português");
        nota(por, 1, "1", "10", "3");
        aulas(por, 4, 4);

        boletim(bearer(Role.SECRETARIA), studentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                .andExpect(jsonPath("$.studentName").value("Ana " + sufixo))
                .andExpect(jsonPath("$.className").value("T" + sufixo))
                .andExpect(jsonPath("$.schoolYear").value(ANO))
                .andExpect(jsonPath("$.result").value("RECOVERY"))
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].subjectName").value("Matemática " + sufixo))
                .andExpect(jsonPath("$.subjects[0].average").value(8.67))
                .andExpect(jsonPath("$.subjects[0].terms.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].terms[0].term").value(1))
                .andExpect(jsonPath("$.subjects[0].terms[0].average").value(8.0))
                .andExpect(jsonPath("$.subjects[0].terms[1].average").value(10.0))
                .andExpect(jsonPath("$.subjects[0].lessons").value(4))
                .andExpect(jsonPath("$.subjects[0].attendance").value(100.0))
                .andExpect(jsonPath("$.subjects[0].result").value("APPROVED"))
                .andExpect(jsonPath("$.subjects[1].subjectName").value("Português " + sufixo))
                .andExpect(jsonPath("$.subjects[1].result").value("RECOVERY"));
    }

    @Test
    void faltasAcimaDoLimiteReprovamMesmoComNotaAlta() throws Exception {
        long mat = disciplina("Matemática");
        nota(mat, 1, "1", "10", "10");
        aulas(mat, 4, 2);

        boletim(bearer(Role.ADMIN), studentId)
                .andExpect(jsonPath("$.subjects[0].average").value(10.0))
                .andExpect(jsonPath("$.subjects[0].attendance").value(50.0))
                .andExpect(jsonPath("$.subjects[0].result").value("FAILED"))
                .andExpect(jsonPath("$.result").value("FAILED"));
    }

    @Test
    void semNotasNemAulasFicaEmAndamento() throws Exception {
        disciplina("Matemática");

        boletim(bearer(Role.ADMIN), studentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].average").value((Object) null))
                .andExpect(jsonPath("$.subjects[0].terms.length()").value(0))
                .andExpect(jsonPath("$.subjects[0].result").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.result").value("IN_PROGRESS"));
    }

    @Test
    void notasDeOutraMatriculaNaoEntramNoBoletim() throws Exception {
        long mat = disciplina("Matemática");
        nota(mat, 1, "1", "10", "9");
        long outroAluno = novoAluno("Bruno " + sufixo);
        long outraMatricula = enrollments.save(new Enrollment(null, outroAluno, classId, ANO, EnrollmentStatus.ACTIVE, Instant.now())).id();
        long avaliacao = assessments.findByClassSubject(mat).get(0).id();
        assessments.saveGrades(List.of(new Grade(null, avaliacao, outraMatricula, new BigDecimal("2"))));

        boletim(bearer(Role.ADMIN), studentId).andExpect(jsonPath("$.subjects[0].average").value(9.0));
        boletim(bearer(Role.ADMIN), outroAluno).andExpect(jsonPath("$.subjects[0].average").value(2.0));
    }

    // ---------- permissões e erros ----------

    @Test
    void professorDaTurmaVeEOutroProfessorRecebe404() throws Exception {
        disciplina("Matemática");

        boletim(bearer(Role.PROFESSOR), studentId).andExpect(status().isOk());
        boletim(bearerOutroProfessor(), studentId).andExpect(status().isNotFound());
    }

    @Test
    void alunoInexistenteOuSemMatriculaAtivaNoAnoDa404() throws Exception {
        long semMatricula = novoAluno("Sem " + sufixo);
        long cancelada = novoAluno("Cancelada " + sufixo);
        enrollments.save(new Enrollment(null, cancelada, classId, ANO, EnrollmentStatus.CANCELLED, Instant.now()));

        boletim(bearer(Role.ADMIN), 999999999L).andExpect(status().isNotFound());
        boletim(bearer(Role.ADMIN), semMatricula).andExpect(status().isNotFound());
        boletim(bearer(Role.ADMIN), cancelada).andExpect(status().isNotFound());
        mvc.perform(get("/students/" + studentId + "/report-card").param("schoolYear", String.valueOf(ANO - 1))
                        .header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anoInvalidoDa400ESemTokenDa401() throws Exception {
        mvc.perform(get("/students/" + studentId + "/report-card").param("schoolYear", "abc")
                        .header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/students/" + studentId + "/report-card")).andExpect(status().isUnauthorized());
    }

    // ---------- N+1 ----------

    /** Comandos SQL que o pedido do boletim dispara (a autenticação já foi feita antes de zerar o contador). */
    private long comandosDoBoletim() throws Exception {
        String auth = bearer(Role.ADMIN);
        var estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();
        boletim(auth, studentId).andExpect(status().isOk());
        return estatisticas.getPrepareStatementCount();
    }

    @Test
    void numeroDeConsultasDoBoletimNaoCresceComDisciplinasEAvaliacoes() throws Exception {
        long vinculo = disciplina("Única");
        nota(vinculo, 1, "1", "10", "7");
        aulas(vinculo, 4, 4);
        long pequeno = comandosDoBoletim();

        for (int d = 0; d < 5; d++) {
            long v = disciplina("D" + d);
            for (int a = 0; a < 4; a++) {
                nota(v, a + 1, "1", "10", "6");
            }
            aulas(v, 4, 4);
        }
        long grande = comandosDoBoletim();

        // 6 disciplinas e 21 avaliações custam o mesmo número de comandos que 1 disciplina e 1 avaliação
        assertThat(grande).as("comandos SQL: 1 disciplina = %d, 6 disciplinas = %d", pequeno, grande).isEqualTo(pequeno);
        // aluno, matrícula, turma, frequência, notas e disciplinas
        assertThat(grande).isEqualTo(6);
    }
}
