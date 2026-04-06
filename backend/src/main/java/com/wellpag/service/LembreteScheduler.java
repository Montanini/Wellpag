package com.wellpag.service;

import com.wellpag.repository.ConfiguracaoWhatsAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LembreteScheduler {

    private final ConfiguracaoWhatsAppRepository configRepo;
    private final WhatsAppService whatsAppService;

    /**
     * Percorre todos os professores com WhatsApp conectado e dispara lembretes.
     * Horário configurável via WHATSAPP_CRON (padrão: todo dia às 09h).
     */
    @Scheduled(cron = "${wellpag.whatsapp.scheduler-cron}")
    public void dispararLembretes() {
        log.info("Scheduler de lembretes WhatsApp iniciado");

        configRepo.findByConectadoTrue().forEach(config -> {
            try {
                int enviados = whatsAppService.enviarLembretes(config.getProfessorId());
                log.info("Professor={} lembretes enviados={}", config.getProfessorId(), enviados);
            } catch (Exception e) {
                log.error("Erro ao enviar lembretes para professor={}: {}",
                    config.getProfessorId(), e.getMessage());
            }
        });

        log.info("Scheduler de lembretes WhatsApp finalizado");
    }
}
