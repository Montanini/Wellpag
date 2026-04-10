package com.wellpag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MensalidadeScheduler {

    private final MensalidadeService mensalidadeService;

    /**
     * Todo dia 1º do mês às 00h05, gera mensalidades em lote para todos os alunos.
     * Alunos que já possuem mensalidade para o mês são ignorados.
     */
    @Scheduled(cron = "${wellpag.mensalidade.scheduler-cron}")
    public void gerarMensalidadesMensais() {
        YearMonth mesAtual = YearMonth.now();
        log.info("Scheduler de mensalidades iniciado para {}", mesAtual);

        try {
            int criadas = mensalidadeService.gerarMensalidadesEmLote(mesAtual);
            log.info("Mensalidades geradas={} para mes={}", criadas, mesAtual);
        } catch (Exception e) {
            log.error("Erro ao gerar mensalidades em lote para mes={}: {}", mesAtual, e.getMessage(), e);
        }
    }
}
