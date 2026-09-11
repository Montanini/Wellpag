package com.wellpag.pagamento.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.PropertyValueConverterFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * Habilita injecao de dependencia nos conversores {@code @ValueConverter}
 * (usados por BancoConfiguracaoInter para cifrar campos sensiveis — ver
 * AesGcmStringConverter). Por padrao, o Spring Data MongoDB instancia essas
 * classes via construtor sem argumentos, sem passar pelo container Spring;
 * registrando aqui um {@code PropertyValueConverterFactory} "bean-factory
 * aware", ele passa a procurar primeiro um bean Spring do tipo do conversor
 * (permitindo, por exemplo, {@code @Value} injetando a chave AES) antes de
 * cair no fallback padrao.
 * <p>
 * Esta e a unica mudanca de configuracao Mongo deste modulo — nao substitui
 * nem estende a autoconfiguracao padrao do Spring Boot para Mongo
 * (MongoAutoConfiguration/MongoDataAutoConfiguration continuam donas do
 * MongoClient/MongoTemplate); apenas fornece o bean MongoCustomConversions
 * que o Spring Boot ja espera poder sobrescrever
 * (@ConditionalOnMissingBean).
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions(BeanFactory beanFactory) {
        return MongoCustomConversions.create(configurationAdapter ->
            configurationAdapter.registerPropertyValueConverterFactory(
                PropertyValueConverterFactory.beanFactoryAware(beanFactory)));
    }
}
