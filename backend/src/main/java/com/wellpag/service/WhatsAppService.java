package com.wellpag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wellpag.dto.WhatsAppConexaoResponse;
import com.wellpag.dto.WhatsAppConfiguracaoRequest;
import com.wellpag.dto.WhatsAppConfiguracaoResponse;
import com.wellpag.model.*;
import com.wellpag.model.LembreteEnviado.TipoLembrete;
import com.wellpag.repository.*;
import com.wellpag.whatsapp.EvolutionApiClient;
import com.wellpag.whatsapp.MensagemTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final ConfiguracaoWhatsAppRepository configRepo;
    private final LembreteEnviadoRepository lembreteRepo;
    private final AlunoRepository alunoRepository;
    private final MensalidadeRepository mensalidadeRepository;
    private final EvolutionApiClient evolutionApi;

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ── Gestão da instância ──────────────────────────────────────────────────

    public WhatsAppConexaoResponse conectar(String professorId) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElseGet(() -> {
                ConfiguracaoWhatsApp nova = new ConfiguracaoWhatsApp();
                nova.setProfessorId(professorId);
                nova.setInstanceName("wellpag-" + professorId.substring(0, Math.min(8, professorId.length())));
                return nova;
            });

        JsonNode resposta = evolutionApi.criarInstancia(config.getInstanceName());
        String qrCode = extrairQrCode(resposta);

        config.setConectado(false);
        configRepo.save(config);

        return new WhatsAppConexaoResponse(config.getInstanceName(), "connecting", qrCode);
    }

    public WhatsAppConexaoResponse status(String professorId) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElse(null);

        if (config == null) {
            return new WhatsAppConexaoResponse(null, "desconectado", null);
        }

        String estado = evolutionApi.estadoConexao(config.getInstanceName());
        boolean conectado = "open".equals(estado);

        if (conectado != config.isConectado()) {
            config.setConectado(conectado);
            configRepo.save(config);
        }

        String qrCode = null;
        if ("connecting".equals(estado)) {
            JsonNode qr = evolutionApi.obterQrCode(config.getInstanceName());
            qrCode = extrairQrCode(qr);
        }

        return new WhatsAppConexaoResponse(config.getInstanceName(), estado, qrCode);
    }

    public void desconectar(String professorId) {
        configRepo.findByProfessorId(professorId).ifPresent(config -> {
            evolutionApi.deletarInstancia(config.getInstanceName());
            config.setConectado(false);
            configRepo.save(config);
        });
    }

    public WhatsAppConfiguracaoResponse obterConfiguracao(String professorId) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElse(new ConfiguracaoWhatsApp());
        return WhatsAppConfiguracaoResponse.from(config);
    }

    public WhatsAppConfiguracaoResponse salvarConfiguracao(String professorId,
                                                            WhatsAppConfiguracaoRequest request) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElseGet(() -> {
                ConfiguracaoWhatsApp nova = new ConfiguracaoWhatsApp();
                nova.setProfessorId(professorId);
                nova.setInstanceName("wellpag-" + professorId.substring(0, Math.min(8, professorId.length())));
                return nova;
            });

        config.setDiasAntesVencimento(request.diasAntesVencimento());
        config.setEnviarAtrasados(request.enviarAtrasados());
        return WhatsAppConfiguracaoResponse.from(configRepo.save(config));
    }

    // ── Envio de lembretes ───────────────────────────────────────────────────

    /** Chamado pelo scheduler ou manualmente pelo professor. */
    public int enviarLembretes(String professorId) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElse(null);

        if (config == null || !config.isConectado()) return 0;

        int enviados = 0;
        String mesAtual = YearMonth.now().format(MES_FMT);
        LocalDate hoje = LocalDate.now();

        List<Aluno> alunos = alunoRepository.findByProfessorId(professorId);

        for (Aluno aluno : alunos) {
            if (aluno.getTelefone() == null || aluno.getTelefone().isBlank()) continue;

            Mensalidade mensalidade = mensalidadeRepository
                .findByAlunoIdAndMesReferencia(aluno.getId(), mesAtual)
                .orElse(null);

            if (mensalidade == null || mensalidade.getStatus() == StatusMensalidade.PAGO) continue;

            TipoLembrete tipo = resolverTipo(mensalidade, aluno, hoje, config);
            if (tipo == null) continue;

            // Anti-spam: máximo 1 lembrete do mesmo tipo por dia
            if (lembreteRepo.existsByAlunoIdAndMesReferenciaAndTipoAndEnviadoEm(
                    aluno.getId(), mesAtual, tipo, hoje)) continue;

            String mensagem = MensagemTemplate.gerar(aluno, mesAtual, tipo);
            boolean ok = evolutionApi.enviarTexto(config.getInstanceName(), aluno.getTelefone(), mensagem);

            if (ok) {
                registrarLembrete(professorId, aluno.getId(), mesAtual, tipo);
                enviados++;
                log.info("Lembrete {} enviado para aluno={}", tipo, aluno.getId());
            }
        }

        return enviados;
    }

    /** Envia lembrete para um aluno específico (disparo manual). */
    public boolean enviarParaAluno(String professorId, String alunoId) {
        ConfiguracaoWhatsApp config = configRepo.findByProfessorId(professorId)
            .orElseThrow(() -> new IllegalArgumentException("WhatsApp não configurado"));

        if (!config.isConectado()) throw new IllegalStateException("WhatsApp não está conectado");

        Aluno aluno = alunoRepository.findByIdAndProfessorId(alunoId, professorId)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        if (aluno.getTelefone() == null || aluno.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Aluno não possui telefone cadastrado");
        }

        String mesAtual = YearMonth.now().format(MES_FMT);
        Mensalidade mensalidade = mensalidadeRepository
            .findByAlunoIdAndMesReferencia(aluno.getId(), mesAtual)
            .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada"));

        TipoLembrete tipo = mensalidade.getStatus() == StatusMensalidade.ATRASADO
            ? TipoLembrete.ATRASADO
            : TipoLembrete.PRE_VENCIMENTO;

        String mensagem = MensagemTemplate.gerar(aluno, mesAtual, tipo);
        boolean ok = evolutionApi.enviarTexto(config.getInstanceName(), aluno.getTelefone(), mensagem);

        if (ok) registrarLembrete(professorId, alunoId, mesAtual, tipo);
        return ok;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TipoLembrete resolverTipo(Mensalidade m, Aluno aluno, LocalDate hoje,
                                       ConfiguracaoWhatsApp config) {
        if (m.getStatus() == StatusMensalidade.ATRASADO && config.isEnviarAtrasados()) {
            return TipoLembrete.ATRASADO;
        }
        if (m.getStatus() == StatusMensalidade.A_PAGAR && aluno.getDiaVencimento() != null) {
            LocalDate vencimento = YearMonth.now().atDay(aluno.getDiaVencimento());
            long diasRestantes = hoje.until(vencimento, java.time.temporal.ChronoUnit.DAYS);
            if (diasRestantes >= 0 && diasRestantes <= config.getDiasAntesVencimento()) {
                return TipoLembrete.PRE_VENCIMENTO;
            }
        }
        return null;
    }

    private void registrarLembrete(String professorId, String alunoId, String mes, TipoLembrete tipo) {
        LembreteEnviado lembrete = new LembreteEnviado();
        lembrete.setProfessorId(professorId);
        lembrete.setAlunoId(alunoId);
        lembrete.setMesReferencia(mes);
        lembrete.setTipo(tipo);
        lembrete.setEnviadoEm(LocalDate.now());
        lembreteRepo.save(lembrete);
    }

    private String extrairQrCode(JsonNode node) {
        if (node == null) return null;
        JsonNode qr = node.path("qrcode");
        if (!qr.isMissingNode()) return qr.path("base64").asText(null);
        return node.path("base64").asText(null);
    }
}
