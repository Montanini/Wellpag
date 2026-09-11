package com.wellpag.pagamento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.convert.ValueConversionContext;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Conversor AES-256-GCM transparente aplicado via {@code @ValueConverter} do
 * Spring Data MongoDB — cifra no write() (save) e decifra no read() (find),
 * sem que BancoInterService precise saber disso (ver BancoConfiguracaoInter).
 * <p>
 * IV de 12 bytes gerado aleatoriamente a cada write() (nunca reaproveitado),
 * prefixado ao ciphertext; o resultado (iv + ciphertext + tag GCM) e
 * codificado em base64 como uma unica string para persistir no Mongo.
 * <p>
 * Registrado como bean Spring (não instanciado via construtor sem argumentos
 * pelo Spring Data) para receber a chave via {@code @Value} — ver
 * {@link MongoConfig}, que registra um
 * {@code PropertyValueConverterFactory.beanFactoryAware(...)} para permitir
 * essa injecao de dependencia nos conversores de propriedade.
 */
@Component
public class AesGcmStringConverter implements PropertyValueConverter<String, String, ValueConversionContext<?>> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmStringConverter(@Value("${wellpag.crypto.banco-config-key}") String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 32) {
            throw new IllegalStateException(
                "wellpag.crypto.banco-config-key deve ser uma chave AES-256 (32 bytes) codificada em base64 "
                    + "(encontrado " + decoded.length + " bytes)");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public String read(String stored, ValueConversionContext<?> context) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar campo protegido de BancoConfiguracaoInter", e);
        }
    }

    @Override
    public String write(String plain, ValueConversionContext<?> context) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar campo protegido de BancoConfiguracaoInter", e);
        }
    }
}
