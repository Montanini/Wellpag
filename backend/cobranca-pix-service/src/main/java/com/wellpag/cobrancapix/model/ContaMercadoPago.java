package com.wellpag.cobrancapix.model;

import com.wellpag.cobrancapix.config.AesGcmStringConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ValueConverter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Conta Mercado Pago conectada de um professor via OAuth Connect (issue #35).
 * Dado 100% de propriedade deste servico (nao e uma copia read-only de outro
 * dominio) — por isso o repositorio abaixo e um {@code MongoRepository}
 * normal, diferente do padrao de bridge read-only (Repository puro +
 * MongoRepository sem save/delete) usado quando um servico le dado de
 * propriedade de outro (ver CLAUDE.md raiz, "Read-only cross-domain bridge").
 * <p>
 * accessToken/refreshToken sao cifrados em repouso de forma transparente via
 * {@link AesGcmStringConverter} (registrado como bean em MongoConfig) — o
 * Spring Data cifra no save() e decifra no find(), sem nenhum codigo adicional
 * nas camadas de service/controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contas_mercadopago")
public class ContaMercadoPago {

    @Id
    private String id;

    @Indexed(unique = true)
    private String professorId;

    private String mpUserId;

    @ValueConverter(AesGcmStringConverter.class)
    private String accessToken;

    @ValueConverter(AesGcmStringConverter.class)
    private String refreshToken;

    private Instant tokenExpiresAt;

    private String publicKey;

    private StatusContaMercadoPago status;

    private Instant createdAt;

    private Instant updatedAt;
}
