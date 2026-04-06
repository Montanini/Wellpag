package com.wellpag.service;

import com.wellpag.dto.DashboardAlunoItem;
import com.wellpag.model.*;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.HorarioRepository;
import com.wellpag.repository.MensalidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final HorarioRepository horarioRepository;
    private final AlunoRepository alunoRepository;
    private final MensalidadeRepository mensalidadeRepository;

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Retorna alunos do professor para o dia da semana informado,
     * com status de mensalidade do mês atual.
     */
    public List<DashboardAlunoItem> listar(String professorId, DiaSemana diaSemana) {
        List<Horario> horarios = horarioRepository.findByProfessorIdAndDiaSemana(professorId, diaSemana);

        if (horarios.isEmpty()) return List.of();

        List<String> alunoIds = horarios.stream().map(Horario::getAlunoId).distinct().toList();

        Map<String, Aluno> alunoMap = alunoRepository.findAllById(alunoIds)
            .stream()
            .collect(Collectors.toMap(Aluno::getId, a -> a));

        String mesAtual = YearMonth.now().format(MES_FMT);

        Map<String, StatusMensalidade> statusMap = mensalidadeRepository
            .findByProfessorIdAndMesReferencia(professorId, mesAtual)
            .stream()
            .collect(Collectors.toMap(Mensalidade::getAlunoId, Mensalidade::getStatus));

        return horarios.stream()
            .map(horario -> {
                Aluno aluno = alunoMap.get(horario.getAlunoId());
                if (aluno == null) return null;

                StatusMensalidade status = statusMap.getOrDefault(
                    aluno.getId(),
                    calcularStatusDefault(aluno.getDiaVencimento())
                );

                return new DashboardAlunoItem(
                    aluno.getId(),
                    aluno.getNome(),
                    aluno.getTelefone(),
                    horario.getHoraInicio(),
                    horario.getHoraFim(),
                    status,
                    mesAtual
                );
            })
            .filter(item -> item != null)
            .sorted((a, b) -> a.horaInicio().compareTo(b.horaInicio()))
            .toList();
    }

    private StatusMensalidade calcularStatusDefault(Integer diaVencimento) {
        if (diaVencimento == null) return StatusMensalidade.A_PAGAR;
        LocalDate vencimento = YearMonth.now().atDay(diaVencimento);
        return LocalDate.now().isAfter(vencimento) ? StatusMensalidade.ATRASADO : StatusMensalidade.A_PAGAR;
    }
}
