package com.wellpag.service;

import com.wellpag.dto.EvolucaoMensalItem;
import com.wellpag.dto.InadimplenteItem;
import com.wellpag.dto.RelatorioResumoResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final MensalidadeRepository mensalidadeRepository;
    private final AlunoRepository alunoRepository;

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public RelatorioResumoResponse resumo(String professorId, YearMonth mes) {
        List<Mensalidade> mensalidades = mensalidadeRepository
            .findByProfessorIdAndMesReferencia(professorId, mes.format(MES_FMT));

        // Recalcula status antes de agregar
        mensalidades = mensalidades.stream().map(this::recalcularStatus).toList();

        int totalAlunos   = alunoRepository.findByProfessorId(professorId).size();
        double esperado   = mensalidades.stream().mapToDouble(m -> m.getValor() != null ? m.getValor() : 0).sum();
        double recebido   = somarPorStatus(mensalidades, StatusMensalidade.PAGO);
        double aPagar     = somarPorStatus(mensalidades, StatusMensalidade.A_PAGAR);
        double atrasado   = somarPorStatus(mensalidades, StatusMensalidade.ATRASADO);
        double percentual = esperado > 0 ? (recebido / esperado) * 100 : 0;

        return new RelatorioResumoResponse(
            mes.format(MES_FMT), totalAlunos,
            round(esperado), round(recebido), round(aPagar), round(atrasado), round(percentual)
        );
    }

    public List<EvolucaoMensalItem> evolucao(String professorId, int quantidadeMeses) {
        List<EvolucaoMensalItem> resultado = new ArrayList<>();
        YearMonth mesAtual = YearMonth.now();

        for (int i = quantidadeMeses - 1; i >= 0; i--) {
            YearMonth mes = mesAtual.minusMonths(i);
            String mesRef = mes.format(MES_FMT);

            List<Mensalidade> mensalidades = mensalidadeRepository
                .findByProfessorIdAndMesReferencia(professorId, mesRef)
                .stream().map(this::recalcularStatus).toList();

            double esperado = mensalidades.stream().mapToDouble(m -> m.getValor() != null ? m.getValor() : 0).sum();
            double recebido = somarPorStatus(mensalidades, StatusMensalidade.PAGO);

            resultado.add(new EvolucaoMensalItem(mesRef, round(esperado), round(recebido)));
        }

        return resultado;
    }

    public List<InadimplenteItem> inadimplentes(String professorId, YearMonth mes) {
        List<Mensalidade> atrasadas = mensalidadeRepository
            .findByProfessorIdAndMesReferencia(professorId, mes.format(MES_FMT))
            .stream()
            .map(this::recalcularStatus)
            .filter(m -> m.getStatus() == StatusMensalidade.ATRASADO)
            .toList();

        Map<String, Aluno> alunoMap = alunoRepository.findByProfessorId(professorId)
            .stream().collect(Collectors.toMap(Aluno::getId, a -> a));

        // Agrupa por aluno (pode ter múltiplos meses atrasados)
        Map<String, List<Mensalidade>> porAluno = atrasadas.stream()
            .collect(Collectors.groupingBy(Mensalidade::getAlunoId));

        return porAluno.entrySet().stream()
            .map(entry -> {
                String alunoId = entry.getKey();
                List<Mensalidade> lista = entry.getValue();
                Aluno aluno = alunoMap.get(alunoId);
                if (aluno == null) return null;

                double total = lista.stream().mapToDouble(m -> m.getValor() != null ? m.getValor() : 0).sum();
                return new InadimplenteItem(
                    alunoId, aluno.getNome(), aluno.getTelefone(),
                    lista.size(), round(total)
                );
            })
            .filter(item -> item != null)
            .sorted((a, b) -> Double.compare(b.totalAtrasado(), a.totalAtrasado()))
            .toList();
    }

    // ---

    private double somarPorStatus(List<Mensalidade> lista, StatusMensalidade status) {
        return lista.stream()
            .filter(m -> m.getStatus() == status)
            .mapToDouble(m -> m.getValor() != null ? m.getValor() : 0)
            .sum();
    }

    private Mensalidade recalcularStatus(Mensalidade m) {
        if (m.getStatus() == StatusMensalidade.PAGO) return m;
        if (m.getDiaVencimento() == null) return m;

        YearMonth mes = YearMonth.parse(m.getMesReferencia(), MES_FMT);
        LocalDate vencimento = mes.atDay(m.getDiaVencimento());
        m.setStatus(LocalDate.now().isAfter(vencimento)
            ? StatusMensalidade.ATRASADO
            : StatusMensalidade.A_PAGAR);
        return m;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
