package com.projetocore.payment.infrastructure.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Constrói um {@link WebClient} com o mesmo comportamento de serialização de
 * {@link com.projetocore.payment.infrastructure.config.WebClientConfig} — sem isso, testes que
 * montam o WebClient "cru" (fora de um contexto Spring) serializam {@code LocalDateTime} como
 * array em vez de string ISO, mascarando o mesmo bug que essa config corrige em produção.
 */
public final class WebClientTestFactory {

    private WebClientTestFactory() {
    }

    public static WebClient build(String baseUrl) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();
    }
}
