package com.wellpag.webhook;

import java.time.LocalDateTime;

public record PayloadExtraido(
    Double valor,
    String nomePagador,
    String documentoPagador,
    LocalDateTime dataTransacao,
    String endToEndId,
    String descricao
) {}
