package com.wellpag.service;

import com.wellpag.dto.AlterarStatusMensalidadeRequest;
import com.wellpag.dto.ConfirmarPagamentoRequest;
import com.wellpag.dto.MensalidadeResponse;
import com.wellpag.model.Aluno;
import com.wellpag.model.Mensalidade;
import com.wellpag.model.StatusMensalidade;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.MensalidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MensalidadeService {

    private final MensalidadeRepository mensalidadeRepository;
    private final AlunoRepository alunoRepository;

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Retorna a mensalidade do mês referência para o aluno.
     * Cria o documento se ainda não existir (lazy creation).
     */
    public MensalidadeResponse buscarOuCriar(String alunoId, String professorId, YearMonth mes) {
        String mesRef = mes.format(MES_FMT);

        return mensalidadeRepository.findByAlunoIdAndMesReferencia(alunoId, mesRef)
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .orElseGet(() -> MensalidadeResponse.from(criarMensalidade(alunoId, professorId, mes)));
    }

    public List<MensalidadeResponse> listarPorAluno(String alunoId, String professorId) {
        return mensalidadeRepository.findByAlunoIdAndProfessorId(alunoId, professorId)
            .stream()
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .toList();
    }

    public List<MensalidadeResponse> listarPorProfessorEMes(String professorId, YearMonth mes) {
        return mensalidadeRepository.findByProfessorIdAndMesReferencia(professorId, mes.format(MES_FMT))
            .stream()
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .toList();
    }

    /** Professor altera o status manualmente. */
    public MensalidadeResponse alterarStatus(String mensalidadeId, String professorId,
                                              AlterarStatusMensalidadeRequest request) {
        Mensalidade m = mensalidadeRepository.findById(mensalidadeId)
            .filter(mens -> mens.getProfessorId().equals(professorId))
            .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada"));

        m.setStatus(request.status());
        if (request.status() != StatusMensalidade.PAGO) {
            m.setDataPagamento(null);
        }

        return MensalidadeResponse.from(mensalidadeRepository.save(m));
    }

    /** Professor confirma o recebimento. */
    public MensalidadeResponse confirmarPagamento(String mensalidadeId, String professorId,
                                                   ConfirmarPagamentoRequest request) {
        Mensalidade m = mensalidadeRepository.findById(mensalidadeId)
            .filter(mens -> mens.getProfessorId().equals(professorId))
            .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada"));

        m.setStatus(StatusMensalidade.PAGO);
        m.setDataPagamento(request.dataPagamento());
        m.setObservacao(request.observacao());

        return MensalidadeResponse.from(mensalidadeRepository.save(m));
    }

    // ---

    private Mensalidade criarMensalidade(String alunoId, String professorId, YearMonth mes) {
        Aluno aluno = alunoRepository.findByIdAndProfessorId(alunoId, professorId)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        Mensalidade m = new Mensalidade();
        m.setAlunoId(alunoId);
        m.setProfessorId(professorId);
        m.setMesReferencia(mes.format(MES_FMT));
        m.setValor(aluno.getValorMensalidade());
        m.setDiaVencimento(aluno.getDiaVencimento());
        m.setStatus(calcularStatus(aluno.getDiaVencimento(), mes));

        return mensalidadeRepository.save(m);
    }

    /**
     * Recalcula status sem persistir — útil para leituras.
     * Respeita: se já PAGO, não altera.
     */
    private Mensalidade recalcularStatus(Mensalidade m) {
        if (m.getStatus() == StatusMensalidade.PAGO) return m;

        YearMonth mes = YearMonth.parse(m.getMesReferencia(), MES_FMT);
        m.setStatus(calcularStatus(m.getDiaVencimento(), mes));
        return m;
    }

    /**
     * Regra: ATRASADO se hoje > data de vencimento; A_PAGAR caso contrário.
     */
    private StatusMensalidade calcularStatus(Integer diaVencimento, YearMonth mes) {
        if (diaVencimento == null) return StatusMensalidade.A_PAGAR;

        LocalDate vencimento = mes.atDay(diaVencimento);
        return LocalDate.now().isAfter(vencimento)
            ? StatusMensalidade.ATRASADO
            : StatusMensalidade.A_PAGAR;
    }
}
