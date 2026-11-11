package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.AttendanceRecord;
import com.modestoneto.secretaria.domain.model.AttendanceSheetEntry;
import com.modestoneto.secretaria.domain.model.AttendanceSummary;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepositoryPort {

    /** Id nulo cria; preenchido atualiza. Lança ConflictException se outra gravação simultânea chegou antes. */
    List<AttendanceRecord> saveAll(List<AttendanceRecord> records);

    List<AttendanceRecord> findByLesson(Long classSubjectId, LocalDate date);

    /** Matrículas ativas da turma com a presença na aula, numa consulta só. */
    List<AttendanceSheetEntry> findSheet(Long classSubjectId, LocalDate date, Long classId);

    List<AttendanceSummary> findSummary(Long enrollmentId);
}
