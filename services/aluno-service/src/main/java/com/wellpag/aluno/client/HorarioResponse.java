package com.wellpag.aluno.client;

import java.time.LocalTime;

/**
 * Subconjunto do response body devolvido por agenda-service em
 * GET /portal/horarios. diaSemana/tipo vêm como String em vez de enum próprio
 * (mesmo padrão de notificacao-service/client/MensalidadeResponse: este
 * serviço só repassa o payload adiante para o frontend via
 * AlunoPortalController, nunca inspeciona esses campos, então não há motivo
 * para duplicar os enums DiaSemana/TipoHorario de agenda-service aqui —
 * Jackson serializa enums como string do mesmo jeito, então o JSON de saída
 * é idêntico ao que o monolito devolvia).
 */
public record HorarioResponse(
    String id,
    String alunoId,
    String diaSemana,
    LocalTime horaInicio,
    LocalTime horaFim,
    String tipo
) {}
