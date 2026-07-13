package com.projetocore.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * {@code WebClient.builder()} sozinho NÃO herda o {@link ObjectMapper} autoconfigurado
     * pelo Spring Boot (que desliga {@code WRITE_DATES_AS_TIMESTAMPS}) — sem passar
     * explicitamente o bean do contexto, campos como {@code LocalDateTime} são serializados
     * como array `[ano,mes,dia,...]`, não como string ISO, o que quebra o contrato do
     * Comprovantes. Descoberto pelo teste de contrato PACT (WireMock não notava, pois as
     * mappings de teste não validam esse campo).
     */
    @Bean
    public WebClient comprovantesWebClient(@Value("${comprovantes.base-url}") String baseUrl,
                                            ObjectMapper objectMapper) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();
    }
}
