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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttendanceControllerTest extends AbstractMySqlIntegrationTest {

    private static final String DIA = "2026-03-10";

    @Autowired MockMvc mvc;
    @Autowired UserRepositoryPort users;
    @Autowired StudentRepositoryPort students;
    @Autowired ClassRepositoryPort classes;
    @Autowired SubjectRepositoryPort subjects;
    @Autowired ClassSubjectRepositoryPort links;
    @Autowired EnrollmentRepositoryPort enrollments;

    private long classId;
    private long subjectId;
    private long enrollA;
    private long enrollB;

    @BeforeEach
    void cenario() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        classId = classes.save(new SchoolClass(null, "T" + sufixo, 5, Shift.MORNING, 2026, 30)).id();
        subjectId = subjects.save(new Subject(null, "Disc " + sufixo)).id();
        TestAuth.tokenDe(mvc, users, Role.PROFESSOR); // garante o professor padrão de teste
        long titular = users.findByEmail("professor@teste.example").orElseThrow().id();
        links.save(new ClassSubject(null, classId, subjectId, titular));
        enrollA = matricular("Ana " + sufixo, EnrollmentStatus.ACTIVE);
        enrollB = matricular("Bruno " + sufixo, EnrollmentStatus.ACTIVE);
    }

    private long matricular(String nome, EnrollmentStatus status) {
        long aluno = students.save(new Student(null, null, nome, LocalDate.of(2014, 5, 10),
                "Responsável", "11999998888", null, true, TestCpf.novo())).id();
        return enrollments.save(new Enrollment(null, aluno, classId, 2026, status, Instant.now())).id();
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

    private String url(String data) {
        return "/classes/" + classId + "/subjects/" + subjectId + "/attendance/" + data;
    }

    private ResultActions registrar(String auth, String data, String corpo) throws Exception {
        return mvc.perform(put(url(data)).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions consultar(String auth, String data) throws Exception {
        return mvc.perform(get(url(data)).header("Authorization", auth));
    }

    private ResultActions resumo(String auth, long matricula) throws Exception {
        return mvc.perform(get("/enrollments/" + matricula + "/attendance").header("Authorization", auth));
    }

    private String presencas(Object... pares) {
        var sb = new StringBuilder("{\"records\":[");
        for (int i = 0; i < pares.length; i += 2) {
            sb.append(i > 0 ? "," : "").append("{\"enrollmentId\":").append(pares[i]).append(",\"present\":").append(pares[i + 1]).append("}");
        }
        return sb.append("]}").toString();
    }

    /** Dispara todas as tarefas ao mesmo tempo e devolve o status HTTP de cada uma. */
    private List<Integer> emParalelo(List<Callable<Integer>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> t : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return t.call();
                }));
            }
            largada.countDown();
            List<Integer> status = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                status.add(f.get());
            }
            return status;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------- registrar e consultar a chamada ----------

    @Test
    void professorTitularRegistraEVeAChamadaAtualizada() throws Exception {
        registrar(bearer(Role.PROFESSOR), DIA, presencas(enrollA, true, enrollB, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].enrollmentId").value(enrollA))
                .andExpect(jsonPath("$[0].present").value(true))
                .andExpect(jsonPath("$[0].studentName").isString())
                .andExpect(jsonPath("$[0].registrationNumber").isString())
                .andExpect(jsonPath("$[1].enrollmentId").value(enrollB))
                .andExpect(jsonPath("$[1].present").value(false));
    }

    @Test
    void aulaSemChamadaTrazOsAlunosComPresencaNula() throws Exception {
        consultar(bearer(Role.PROFESSOR), DIA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].present").value((Object) null))
                .andExpect(jsonPath("$[1].present").value((Object) null));
    }

    @Test
    void regravarAMesmaDataAtualizaSemDuplicar() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        registrar(prof, DIA, presencas(enrollA, true, enrollB, true)).andExpect(status().isOk());

        registrar(prof, DIA, presencas(enrollB, false)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].present").value(true))
                .andExpect(jsonPath("$[1].present").value(false));

        // uma aula só para cada aluno, e a falta de B foi a que valeu
        resumo(bearer(Role.ADMIN), enrollA).andExpect(jsonPath("$[0].lessons").value(1)).andExpect(jsonPath("$[0].presences").value(1));
        resumo(bearer(Role.ADMIN), enrollB).andExpect(jsonPath("$[0].lessons").value(1)).andExpect(jsonPath("$[0].absences").value(1));
    }

    @Test
    void chamadaSoTrazMatriculasAtivas() throws Exception {
        long cancelada = matricular("Cancelada " + UUID.randomUUID(), EnrollmentStatus.CANCELLED);

        consultar(bearer(Role.PROFESSOR), DIA).andExpect(jsonPath("$.length()").value(2));
        registrar(bearer(Role.PROFESSOR), DIA, presencas(cancelada, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Matrícula não está ativa nesta turma: " + cancelada));
    }

    @Test
    void matriculaDeOutraTurmaOuRepetidaDa400() throws Exception {
        String prof = bearer(Role.PROFESSOR);

        registrar(prof, DIA, presencas(999999999L, true)).andExpect(status().isBadRequest());
        registrar(prof, DIA, presencas(enrollA, true, enrollA, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Matrícula repetida: " + enrollA));
        // tudo ou nada: o pedido inválido não gravou nada
        consultar(prof, DIA).andExpect(jsonPath("$[0].present").value((Object) null));
    }

    @Test
    void dataFuturaDa400() throws Exception {
        String amanha = LocalDate.now().plusDays(1).toString();

        registrar(bearer(Role.PROFESSOR), amanha, presencas(enrollA, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possível registrar presença em data futura"));
        registrar(bearer(Role.PROFESSOR), LocalDate.now().toString(), presencas(enrollA, true)).andExpect(status().isOk());
    }

    @Test
    void dataInvalidaDa400() throws Exception {
        String prof = bearer(Role.PROFESSOR);

        registrar(prof, "2026-13-45", presencas(enrollA, true)).andExpect(status().isBadRequest());
        consultar(prof, "10-03-2026").andExpect(status().isBadRequest());
    }

    @Test
    void corpoInvalidoDa400() throws Exception {
        String prof = bearer(Role.PROFESSOR);

        registrar(prof, DIA, "{}").andExpect(status().isBadRequest());
        registrar(prof, DIA, "{\"records\":[]}").andExpect(status().isBadRequest());
        registrar(prof, DIA, "{\"records\":[{\"enrollmentId\":" + enrollA + "}]}").andExpect(status().isBadRequest());
        registrar(prof, DIA, "{\"records\":[{\"present\":true}]}").andExpect(status().isBadRequest());
        registrar(prof, DIA, "{\"records\":[{\"enrollmentId\":" + enrollA + ",\"present\":\"talvez\"}]}").andExpect(status().isBadRequest());
    }

    // ---------- permissões ----------

    @Test
    void gestaoConsultaMasNaoRegistra() throws Exception {
        registrar(bearer(Role.PROFESSOR), DIA, presencas(enrollA, true)).andExpect(status().isOk());

        consultar(bearer(Role.SECRETARIA), DIA).andExpect(status().isOk()).andExpect(jsonPath("$[0].present").value(true));
        consultar(bearer(Role.ADMIN), DIA).andExpect(status().isOk());
        registrar(bearer(Role.SECRETARIA), DIA, presencas(enrollA, false)).andExpect(status().isForbidden());
        registrar(bearer(Role.ADMIN), DIA, presencas(enrollA, false)).andExpect(status().isForbidden());
    }

    @Test
    void professorQueNaoEOTitularRecebe404() throws Exception {
        String outro = bearerOutroProfessor();

        registrar(outro, DIA, presencas(enrollA, true)).andExpect(status().isNotFound());
        consultar(outro, DIA).andExpect(status().isNotFound());
    }

    @Test
    void disciplinaNaoVinculadaDa404() throws Exception {
        long outraDisciplina = subjects.save(new Subject(null, "Solta " + UUID.randomUUID())).id();

        mvc.perform(get("/classes/" + classId + "/subjects/" + outraDisciplina + "/attendance/" + DIA)
                        .header("Authorization", bearer(Role.SECRETARIA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get(url(DIA))).andExpect(status().isUnauthorized());
        mvc.perform(put(url(DIA)).contentType(MediaType.APPLICATION_JSON).content(presencas(enrollA, true)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/enrollments/" + enrollA + "/attendance")).andExpect(status().isUnauthorized());
    }

    // ---------- frequência do aluno ----------

    @Test
    void resumoSomaAulasPresencasEFaltasPorDisciplina() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        registrar(prof, "2026-03-10", presencas(enrollA, true)).andExpect(status().isOk());
        registrar(prof, "2026-03-11", presencas(enrollA, true)).andExpect(status().isOk());
        registrar(prof, "2026-03-12", presencas(enrollA, false)).andExpect(status().isOk());

        resumo(bearer(Role.SECRETARIA), enrollA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subjectId").value(subjectId))
                .andExpect(jsonPath("$[0].subjectName").isString())
                .andExpect(jsonPath("$[0].lessons").value(3))
                .andExpect(jsonPath("$[0].presences").value(2))
                .andExpect(jsonPath("$[0].absences").value(1))
                .andExpect(jsonPath("$[0].percentage").value(66.7));
    }

    @Test
    void semAulasRegistradasOResumoVemVazio() throws Exception {
        resumo(bearer(Role.ADMIN), enrollA).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void resumoEDaGestaoEMatriculaInexistenteDa404() throws Exception {
        resumo(bearer(Role.PROFESSOR), enrollA).andExpect(status().isForbidden());
        resumo(bearer(Role.ADMIN), 999999999L).andExpect(status().isNotFound());
    }

    // ---------- concorrência ----------

    @Test
    void chamadasSimultaneasDaMesmaAulaNaoDuplicamRegistros() throws Exception {
        String prof = bearer(Role.PROFESSOR);
        List<Callable<Integer>> tarefas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            boolean presente = i % 2 == 0;
            tarefas.add(() -> registrar(prof, DIA, presencas(enrollA, presente, enrollB, !presente))
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> status = emParalelo(tarefas);

        // a trava no vínculo faz uma passar de cada vez: todas dão 200 e cada aluno tem uma aula só
        assertThat(status).containsOnly(200);
        resumo(bearer(Role.ADMIN), enrollA).andExpect(jsonPath("$[0].lessons").value(1));
        resumo(bearer(Role.ADMIN), enrollB).andExpect(jsonPath("$[0].lessons").value(1));
    }
}
