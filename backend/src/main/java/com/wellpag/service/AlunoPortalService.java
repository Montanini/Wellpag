package com.wellpag.service;

import com.wellpag.dto.HorarioResponse;
import com.wellpag.dto.MensalidadeResponse;
import com.wellpag.dto.PortalPerfilResponse;
import com.wellpag.dto.PortalRelatorioResponse;
import com.wellpag.model.Aluno;
import com.wellpag.model.Mensalidade;
import com.wellpag.model.StatusMensalidade;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.HorarioRepository;
import com.wellpag.repository.MensalidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoPortalService {

    private final AlunoRepository alunoRepository;
    private final HorarioRepository horarioRepository;
    private final MensalidadeRepository mensalidadeRepository;

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public List<PortalPerfilResponse> perfis(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream().map(PortalPerfilResponse::from).toList();
    }

    public List<HorarioResponse> horarios(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .flatMap(aluno -> horarioRepository.findByAlunoId(aluno.getId()).stream())
            .map(HorarioResponse::from)
            .toList();
    }

    public List<MensalidadeResponse> mensalidades(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .flatMap(aluno -> buscarOuCriarMensalidadesMes(aluno).stream())
            .toList();
    }

    public List<MensalidadeResponse> mensalidadesPorMes(String usuarioId, YearMonth mes) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .map(aluno -> buscarOuCriarParaMes(aluno, mes))
            .map(MensalidadeResponse::from)
            .toList();
    }

    public PortalRelatorioResponse relatorio(String usuarioId) {
        List<Mensalidade> todas = alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .flatMap(aluno -> mensalidadeRepository.findByAlunoId(aluno.getId()).stream())
            .map(this::recalcularStatus)
            .toList();

        double pago     = somarPorStatus(todas, StatusMensalidade.PAGO);
        double aPagar   = somarPorStatus(todas, StatusMensalidade.A_PAGAR);
        double atrasado = somarPorStatus(todas, StatusMensalidade.ATRASADO);

        return new PortalRelatorioResponse(
            todas.size(), round(pago), round(aPagar), round(atrasado), round(pago + aPagar + atrasado)
        );
    }

    // ---

    private List<MensalidadeResponse> buscarOuCriarMensalidadesMes(Aluno aluno) {
        List<Mensalidade> existentes = mensalidadeRepository.findByAlunoId(aluno.getId());

        String mesAtual = YearMonth.now().format(MES_FMT);
        boolean temMesAtual = existentes.stream().anyMatch(m -> m.getMesReferencia().equals(mesAtual));

        if (!temMesAtual && aluno.getValorMensalidade() != null) {
            criarMensalidade(aluno, YearMonth.now());
            existentes = mensalidadeRepository.findByAlunoId(aluno.getId());
        }

        return existentes.stream()
            .map(m -> MensalidadeResponse.from(recalcularStatus(m)))
            .sorted((a, b) -> b.mesReferencia().compareTo(a.mesReferencia()))
            .toList();
    }

    private Mensalidade buscarOuCriarParaMes(Aluno aluno, YearMonth mes) {
        return mensalidadeRepository
            .findByAlunoIdAndMesReferencia(aluno.getId(), mes.format(MES_FMT))
            .map(this::recalcularStatus)
            .orElseGet(() -> criarMensalidade(aluno, mes));
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

    private Mensalidade recalcularStatus(Mensalidade m) {
        if (m.getStatus() == StatusMensalidade.PAGO) return m;
        if (m.getDiaVencimento() == null) return m;
        YearMonth mes = YearMonth.parse(m.getMesReferencia(), MES_FMT);
        m.setStatus(calcularStatus(m.getDiaVencimento(), mes));
        return m;
    }

    private StatusMensalidade calcularStatus(Integer diaVencimento, YearMonth mes) {
        if (diaVencimento == null) return StatusMensalidade.A_PAGAR;
        LocalDate vencimento = mes.atDay(diaVencimento);
        return LocalDate.now().isAfter(vencimento) ? StatusMensalidade.ATRASADO : StatusMensalidade.A_PAGAR;
    }

    private double somarPorStatus(List<Mensalidade> lista, StatusMensalidade status) {
        return lista.stream()
            .filter(m -> m.getStatus() == status)
            .mapToDouble(m -> m.getValor() != null ? m.getValor() : 0)
            .sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
