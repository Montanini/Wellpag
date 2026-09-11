package com.wellpag.financeiro.service;

import com.wellpag.financeiro.dto.AlterarStatusMensalidadeRequest;
import com.wellpag.financeiro.dto.ConfirmarPagamentoRequest;
import com.wellpag.financeiro.dto.MensalidadeResponse;
import com.wellpag.financeiro.model.Aluno;
import com.wellpag.financeiro.model.Mensalidade;
import com.wellpag.financeiro.model.StatusMensalidade;
import com.wellpag.financeiro.repository.AlunoRepository;
import com.wellpag.financeiro.repository.MensalidadeRepository;
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

        return mensalidadeRepository.findByAlunoIdAndProfessorIdAndMesReferencia(alunoId, professorId, mesRef)
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

    /**
     * Porte fiel de AlunoPortalService.mensalidades()/buscarOuCriarMensalidadesMes
     * do monolito: para cada Aluno vinculado ao usuarioId autenticado, garante que
     * existe mensalidade do mês atual (cria se não existir E valorMensalidade !=
     * null) e retorna TODAS as mensalidades existentes daquele aluno, ordenadas
     * por mês desc.
     */
    public List<MensalidadeResponse> mensalidadesPortal(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .flatMap(aluno -> buscarOuCriarMensalidadesMesPortal(aluno).stream())
            .toList();
    }

    /**
     * Porte fiel de AlunoPortalService.mensalidadesPorMes()/buscarOuCriarParaMes
     * do monolito: para cada Aluno vinculado, garante/cria a mensalidade do mês
     * informado.
     */
    public List<MensalidadeResponse> mensalidadesPortalPorMes(String usuarioId, YearMonth mes) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .map(aluno -> buscarOuCriarParaMesPortal(aluno, mes))
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .toList();
    }

    /**
     * Gera mensalidades em lote para todos os alunos no mês informado.
     * Ignora alunos que já possuem mensalidade para o mês.
     */
    public int gerarMensalidadesEmLote(YearMonth mes) {
        String mesRef = mes.format(MES_FMT);
        int criadas = 0;

        for (Aluno aluno : alunoRepository.findAll()) {
            boolean jaExiste = mensalidadeRepository
                .findByAlunoIdAndMesReferencia(aluno.getId(), mesRef)
                .isPresent();

            if (!jaExiste) {
                Mensalidade m = new Mensalidade();
                m.setAlunoId(aluno.getId());
                m.setProfessorId(aluno.getProfessorId());
                m.setMesReferencia(mesRef);
                m.setValor(aluno.getValorMensalidade());
                m.setDiaVencimento(aluno.getDiaVencimento());
                m.setStatus(calcularStatus(aluno.getDiaVencimento(), mes));
                mensalidadeRepository.save(m);
                criadas++;
            }
        }

        return criadas;
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

    /**
     * Busca-ou-cria do fluxo do portal do aluno (usado por mensalidadesPortal):
     * mesma lógica de AlunoPortalService.buscarOuCriarMensalidadesMes — usa
     * findByAlunoIdAndProfessorId em vez de findByAlunoId (o monolito tinha um
     * MensalidadeRepository unificado com findByAlunoId isolado; aqui só existe
     * o método com professorId, e como cada mensalidade já é sempre gravada com
     * o professorId do próprio aluno, o resultado é equivalente).
     */
    private List<MensalidadeResponse> buscarOuCriarMensalidadesMesPortal(Aluno aluno) {
        List<Mensalidade> existentes =
            mensalidadeRepository.findByAlunoIdAndProfessorId(aluno.getId(), aluno.getProfessorId());

        String mesAtual = YearMonth.now().format(MES_FMT);
        boolean temMesAtual = existentes.stream().anyMatch(m -> m.getMesReferencia().equals(mesAtual));

        if (!temMesAtual && aluno.getValorMensalidade() != null) {
            criarMensalidade(aluno, YearMonth.now());
            existentes = mensalidadeRepository.findByAlunoIdAndProfessorId(aluno.getId(), aluno.getProfessorId());
        }

        return existentes.stream()
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .sorted((a, b) -> b.mesReferencia().compareTo(a.mesReferencia()))
            .toList();
    }

    /** Porte fiel de AlunoPortalService.buscarOuCriarParaMes. */
    private Mensalidade buscarOuCriarParaMesPortal(Aluno aluno, YearMonth mes) {
        return mensalidadeRepository
            .findByAlunoIdAndMesReferencia(aluno.getId(), mes.format(MES_FMT))
            .map(this::recalcularStatus)
            .orElseGet(() -> criarMensalidade(aluno, mes));
    }

    private Mensalidade criarMensalidade(String alunoId, String professorId, YearMonth mes) {
        Aluno aluno = alunoRepository.findByIdAndProfessorId(alunoId, professorId)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
        return criarMensalidade(aluno, mes);
    }

    private Mensalidade criarMensalidade(Aluno aluno, YearMonth mes) {
        Mensalidade m = new Mensalidade();
        m.setAlunoId(aluno.getId());
        m.setProfessorId(aluno.getProfessorId());
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
