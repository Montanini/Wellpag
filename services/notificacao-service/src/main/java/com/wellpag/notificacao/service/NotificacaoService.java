package com.wellpag.notificacao.service;

import com.wellpag.notificacao.client.FinanceiroServiceClient;
import com.wellpag.notificacao.dto.NotificacaoResponse;
import com.wellpag.notificacao.dto.VincularNotificacaoRequest;
import com.wellpag.notificacao.dto.WebhookConfiguracaoResponse;
import com.wellpag.notificacao.model.NotificacaoPagamento;
import com.wellpag.notificacao.model.StatusNotificacao;
import com.wellpag.notificacao.model.Usuario;
import com.wellpag.notificacao.repository.NotificacaoRepository;
import com.wellpag.notificacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FinanceiroServiceClient financeiroServiceClient;

    @Value("${wellpag.webhook.base-url}")
    private String webhookBaseUrl;

    public WebhookConfiguracaoResponse configuracao(String professorId) {
        Usuario professor = usuarioRepository.findById(professorId)
            .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado"));

        String token = professor.getWebhookToken();
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("PIX (padrão Bacen)", webhookBaseUrl + "/webhook/" + token + "/pix_generico");
        urls.put("Asaas",              webhookBaseUrl + "/webhook/" + token + "/asaas");
        urls.put("Inter",              webhookBaseUrl + "/webhook/" + token + "/inter");
        urls.put("Sicoob",             webhookBaseUrl + "/webhook/" + token + "/sicoob");
        urls.put("Efi/Gerencianet",    webhookBaseUrl + "/webhook/" + token + "/efipay");
        urls.put("Outro banco",        webhookBaseUrl + "/webhook/" + token + "/generico");

        return new WebhookConfiguracaoResponse(token, urls);
    }

    public List<NotificacaoResponse> listar(String professorId, StatusNotificacao status) {
        List<NotificacaoPagamento> lista = status != null
            ? notificacaoRepository.findByProfessorIdAndStatusOrderByRecebidaEmDesc(professorId, status)
            : notificacaoRepository.findByProfessorIdOrderByRecebidaEmDesc(professorId);

        return lista.stream().map(NotificacaoResponse::from).toList();
    }

    public NotificacaoResponse buscar(String notificacaoId, String professorId) {
        return NotificacaoResponse.from(buscarNotificacao(notificacaoId, professorId));
    }

    /**
     * Vincula a notificação a uma mensalidade e confirma o pagamento automaticamente.
     *
     * Diferente das demais escritas deste servico, a confirmacao do pagamento em si
     * NAO e uma escrita local: financeiro-service e o dono real de Mensalidade (ja
     * extraido/mergeado, porta 8095) e ja expoe o endpoint que faz exatamente isso
     * (PATCH /professor/mensalidades/{id}/confirmar). Ver FinanceiroServiceClient
     * para o detalhe dessa decisao. authorization e o header Authorization bruto da
     * requisicao original do professor (repassado pelo controller), reenviado sem
     * alteracao — nenhum token novo e gerado aqui. A validacao de posse
     * (mensalidade pertence a esse professor) e feita do lado do financeiro-service;
     * este servico nao precisa mais consultar Mensalidade localmente para isso.
     */
    public NotificacaoResponse vincular(String notificacaoId, String professorId, String authorization,
                                        VincularNotificacaoRequest request) {
        NotificacaoPagamento notificacao = buscarNotificacao(notificacaoId, professorId);

        LocalDate dataPagamento = notificacao.getDataTransacao() != null
            ? notificacao.getDataTransacao().toLocalDate()
            : LocalDate.now();

        financeiroServiceClient.confirmarPagamento(request.mensalidadeId(), authorization, dataPagamento, null);

        // Atualiza a notificação (dado proprio deste servico)
        notificacao.setStatus(StatusNotificacao.VINCULADA);
        notificacao.setMensalidadeId(request.mensalidadeId());
        notificacao.setAlunoId(request.alunoId());

        return NotificacaoResponse.from(notificacaoRepository.save(notificacao));
    }

    public NotificacaoResponse ignorar(String notificacaoId, String professorId) {
        NotificacaoPagamento notificacao = buscarNotificacao(notificacaoId, professorId);
        notificacao.setStatus(StatusNotificacao.IGNORADA);
        return NotificacaoResponse.from(notificacaoRepository.save(notificacao));
    }

    private NotificacaoPagamento buscarNotificacao(String id, String professorId) {
        return notificacaoRepository.findByIdAndProfessorId(id, professorId)
            .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));
    }
}
