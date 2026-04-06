package com.wellpag.service;

import com.wellpag.model.Aluno;
import com.wellpag.model.BancoIntegracao;
import com.wellpag.model.Mensalidade;
import com.wellpag.model.NotificacaoPagamento;
import com.wellpag.model.StatusMensalidade;
import com.wellpag.model.StatusNotificacao;
import com.wellpag.model.Usuario;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.MensalidadeRepository;
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
    private final AlunoRepository alunoRepository;
    private final MensalidadeRepository mensalidadeRepository;
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

        tentarVincularAutomaticamente(notificacao, professor.getId(), extraido.documentoPagador());
    }

    /**
     * Tenta vincular automaticamente a notificação a um aluno pelo CPF do pagador.
     * Se encontrar o aluno e uma mensalidade pendente, confirma o pagamento.
     * Caso contrário, deixa como PENDENTE para o professor dar baixa manualmente.
     */
    private void tentarVincularAutomaticamente(NotificacaoPagamento notificacao,
                                               String professorId,
                                               String documentoPagador) {
        if (documentoPagador == null || documentoPagador.isBlank()) return;

        String cpfNormalizado = documentoPagador.replaceAll("[^0-9]", "");

        alunoRepository.findByProfessorIdAndCpfPagador(professorId, cpfNormalizado).ifPresent(aluno -> {
            mensalidadeRepository.findByAlunoId(aluno.getId()).stream()
                .filter(m -> m.getStatus() != StatusMensalidade.PAGO)
                .min((a, b) -> a.getMesReferencia().compareTo(b.getMesReferencia()))
                .ifPresent(mensalidade -> {
                    mensalidade.setStatus(StatusMensalidade.PAGO);
                    mensalidade.setDataPagamento(
                        notificacao.getDataTransacao() != null
                            ? notificacao.getDataTransacao().toLocalDate()
                            : java.time.LocalDate.now()
                    );
                    mensalidadeRepository.save(mensalidade);

                    notificacao.setStatus(StatusNotificacao.VINCULADA);
                    notificacao.setAlunoId(aluno.getId());
                    notificacao.setMensalidadeId(mensalidade.getId());
                    notificacaoRepository.save(notificacao);

                    log.info("Pagamento vinculado automaticamente: aluno={} mensalidade={} via CPF={}",
                        aluno.getId(), mensalidade.getId(), cpfNormalizado);
                });
        });
    }

    private BancoIntegracao resolverBanco(String slug) {
        try {
            return BancoIntegracao.valueOf(slug.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return BancoIntegracao.GENERICO;
        }
    }
}
