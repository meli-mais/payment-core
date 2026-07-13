package com.projetocore.payment.infrastructure.adapter.client;

import com.projetocore.payment.domain.exception.ComprovanteIndisponivelException;
import com.projetocore.payment.domain.port.ComprovanteGatewayPort;
import com.projetocore.payment.domain.port.ComprovanteSolicitado;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementação de {@link ComprovanteGatewayPort} via HTTP (constitution.md, Princípio V —
 * aponta para WireMock em dev/test, para o serviço real em produção via
 * {@code comprovantes.base-url}). Protegida por Resilience4j (Princípio IV).
 */
@Component
public class ComprovanteHttpClient implements ComprovanteGatewayPort {

    private final WebClient webClient;

    public ComprovanteHttpClient(WebClient comprovantesWebClient) {
        this.webClient = comprovantesWebClient;
    }

    @Override
    @CircuitBreaker(name = "comprovantes")
    @Retry(name = "comprovantes")
    public ComprovanteSolicitado solicitar(SolicitacaoComprovante dados) {
        try {
            ComprovanteResponse resposta = webClient.post()
                    .uri("/comprovantes")
                    .bodyValue(ComprovanteRequest.de(dados))
                    .retrieve()
                    .bodyToMono(ComprovanteResponse.class)
                    .block();

            if (resposta == null || resposta.identificadorComprovante() == null) {
                throw new ComprovanteIndisponivelException(
                        "Comprovantes retornou 202 sem identificador_comprovante");
            }
            return new ComprovanteSolicitado(UUID.fromString(resposta.identificadorComprovante()), Instant.now());
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw new ComprovanteIndisponivelException(
                    "Falha ao solicitar comprovante: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // identificador_comprovante não é um UUID válido — problema do Comprovantes, não
            // do cliente que chamou este serviço; NÃO deixar vazar como erro de validação
            // (viraria 400 injustamente) — é indisponibilidade da dependência externa.
            throw new ComprovanteIndisponivelException(
                    "Comprovantes retornou identificador_comprovante inválido: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "comprovantes")
    public boolean confirmarPersistencia(UUID comprovanteId) {
        try {
            webClient.get()
                    .uri("/comprovantes/{id}", comprovanteId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true; // 200 OK — comprovante encontrado
        } catch (WebClientResponseException.NotFound e) {
            return false; // 404 — ainda não confirmado nesta tentativa (não é falha técnica)
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw new ComprovanteIndisponivelException(
                    "Falha ao confirmar persistência do comprovante: " + e.getMessage(), e);
        }
    }
}
