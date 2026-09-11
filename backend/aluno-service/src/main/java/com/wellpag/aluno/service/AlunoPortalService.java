package com.wellpag.aluno.service;

import com.wellpag.aluno.client.AgendaServiceClient;
import com.wellpag.aluno.client.FinanceiroServiceClient;
import com.wellpag.aluno.client.HorarioResponse;
import com.wellpag.aluno.client.MensalidadeResponse;
import com.wellpag.aluno.dto.PortalPerfilResponse;
import com.wellpag.aluno.dto.PortalRelatorioResponse;
import com.wellpag.aluno.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra o portal do aluno. Porte fiel de AlunoPortalService do monolito,
 * agora dividido por dono real dos dados:
 * - perfil(): 100% local — aluno-service é o dono real de Aluno.
 * - horarios(): delega para agenda-service via AgendaServiceClient (dono real
 *   de Horario).
 * - mensalidades()/mensalidadesPorMes(): delegam para financeiro-service via
 *   FinanceiroServiceClient (dono real de Mensalidade, inclusive a
 *   lazy-creation do mês atual/mês específico).
 * - relatorio(): busca a mesma fonte de mensalidades() em financeiro-service
 *   (GET /portal/mensalidades) e agrega LOCALMENTE, reproduzindo fielmente a
 *   matemática de somarPorStatus/round do monolito.
 *
 * Em todas as chamadas remotas, o header Authorization da requisição original
 * do aluno é reenviado sem alteração — nenhum token novo é gerado aqui (mesmo
 * padrão de notificacao-service/client/FinanceiroServiceClient).
 */
@Service
@RequiredArgsConstructor
public class AlunoPortalService {

    private final AlunoRepository alunoRepository;
    private final AgendaServiceClient agendaServiceClient;
    private final FinanceiroServiceClient financeiroServiceClient;

    public List<PortalPerfilResponse> perfis(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream().map(PortalPerfilResponse::from).toList();
    }

    public List<HorarioResponse> horarios(String authorization) {
        return agendaServiceClient.horarios(authorization);
    }

    public List<MensalidadeResponse> mensalidades(String authorization) {
        return financeiroServiceClient.mensalidades(authorization);
    }

    public List<MensalidadeResponse> mensalidadesPorMes(String mes, String authorization) {
        return financeiroServiceClient.mensalidadesPorMes(mes, authorization);
    }

    public PortalRelatorioResponse relatorio(String authorization) {
        List<MensalidadeResponse> todas = financeiroServiceClient.mensalidades(authorization);

        double pago     = somarPorStatus(todas, "PAGO");
        double aPagar   = somarPorStatus(todas, "A_PAGAR");
        double atrasado = somarPorStatus(todas, "ATRASADO");

        return new PortalRelatorioResponse(
            todas.size(), round(pago), round(aPagar), round(atrasado), round(pago + aPagar + atrasado)
        );
    }

    // ---

    private double somarPorStatus(List<MensalidadeResponse> lista, String status) {
        return lista.stream()
            .filter(m -> status.equals(m.status()))
            .mapToDouble(m -> m.valor() != null ? m.valor() : 0)
            .sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
