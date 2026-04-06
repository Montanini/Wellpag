package com.wellpag.service;

import com.wellpag.model.BancoIntegracao;
import com.wellpag.model.NotificacaoPagamento;
import com.wellpag.model.StatusNotificacao;
import com.wellpag.model.Usuario;
import com.wellpag.repository.NotificacaoRepository;
import com.wellpag.repository.UsuarioRepository;
import com.wellpag.webhook.BancoParser;
import com.wellpag.webhook.PayloadExtraido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final UsuarioRepository usuarioRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final List<BancoParser> parsers;

    /** Mapa banco → parser, construído na inicialização. */
    private Map<BancoIntegracao, BancoParser> parserMap() {
        return parsers.stream().collect(Collectors.toMap(BancoParser::banco, p -> p));
    }

    public void processar(String professorToken, String bancoSlug, String payloadJson) {
        Usuario professor = usuarioRepository.findByWebhookToken(professorToken)
            .orElseThrow(() -> {
                log.warn("Webhook recebido com token desconhecido: {}", professorToken);
                return new IllegalArgumentException("Token inválido");
            });

        BancoIntegracao banco = resolverBanco(bancoSlug);
        PayloadExtraido extraido = parserMap()
            .getOrDefault(banco, parserMap().get(BancoIntegracao.GENERICO))
            .extrair(payloadJson);

        // Idempotência: ignora duplicatas pelo endToEndId
        if (extraido.endToEndId() != null && notificacaoRepository.existsByEndToEndId(extraido.endToEndId())) {
            log.info("Notificação duplicada ignorada: endToEndId={}", extraido.endToEndId());
            return;
        }

        NotificacaoPagamento notificacao = new NotificacaoPagamento();
        notificacao.setProfessorId(professor.getId());
        notificacao.setBanco(banco);
        notificacao.setPayloadBruto(payloadJson);
        notificacao.setValor(extraido.valor());
        notificacao.setNomePagador(extraido.nomePagador());
        notificacao.setDocumentoPagador(extraido.documentoPagador());
        notificacao.setDataTransacao(extraido.dataTransacao());
        notificacao.setEndToEndId(extraido.endToEndId());
        notificacao.setDescricao(extraido.descricao());
        notificacao.setStatus(StatusNotificacao.PENDENTE);

        notificacaoRepository.save(notificacao);
        log.info("Notificação salva para professor={} banco={} valor={}", professor.getId(), banco, extraido.valor());
    }

    private BancoIntegracao resolverBanco(String slug) {
        try {
            return BancoIntegracao.valueOf(slug.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return BancoIntegracao.GENERICO;
        }
    }
}
