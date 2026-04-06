package com.wellpag.service;

import com.wellpag.dto.NotificacaoResponse;
import com.wellpag.dto.VincularNotificacaoRequest;
import com.wellpag.dto.WebhookConfiguracaoResponse;
import com.wellpag.model.Mensalidade;
import com.wellpag.model.NotificacaoPagamento;
import com.wellpag.model.StatusMensalidade;
import com.wellpag.model.StatusNotificacao;
import com.wellpag.model.Usuario;
import com.wellpag.repository.MensalidadeRepository;
import com.wellpag.repository.NotificacaoRepository;
import com.wellpag.repository.UsuarioRepository;
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
    private final MensalidadeRepository mensalidadeRepository;
    private final UsuarioRepository usuarioRepository;

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
     */
    public NotificacaoResponse vincular(String notificacaoId, String professorId,
                                        VincularNotificacaoRequest request) {
        NotificacaoPagamento notificacao = buscarNotificacao(notificacaoId, professorId);

        Mensalidade mensalidade = mensalidadeRepository.findById(request.mensalidadeId())
            .filter(m -> m.getProfessorId().equals(professorId))
            .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada"));

        // Confirma o pagamento
        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setDataPagamento(
            notificacao.getDataTransacao() != null
                ? notificacao.getDataTransacao().toLocalDate()
                : LocalDate.now()
        );
        mensalidadeRepository.save(mensalidade);

        // Atualiza a notificação
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
