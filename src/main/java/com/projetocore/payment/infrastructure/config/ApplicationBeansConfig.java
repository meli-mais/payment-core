package com.projetocore.payment.infrastructure.config;

import com.projetocore.payment.application.saga.PagamentoSagaOrchestrator;
import com.projetocore.payment.application.usecase.ConsultarFaturaUseCase;
import com.projetocore.payment.application.usecase.SolicitarPagamentoPixUseCase;
import com.projetocore.payment.domain.port.ComprovanteGatewayPort;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiação (wiring) da camada Application — ela própria não conhece Spring
 * (constitution.md, Princípio I), então quem cria os beans é a Infrastructure.
 */
@Configuration
public class ApplicationBeansConfig {

    @Bean
    public PagamentoSagaOrchestrator pagamentoSagaOrchestrator(
            ComprovanteGatewayPort comprovanteGateway,
            FaturaRepositoryPort faturaRepository,
            @Value("${comprovantes.confirmacao.max-tentativas}") int maxTentativasConfirmacao,
            @Value("${comprovantes.confirmacao.intervalo-inicial-ms}") long intervaloEntreTentativasMs) {
        return new PagamentoSagaOrchestrator(comprovanteGateway, faturaRepository,
                maxTentativasConfirmacao, intervaloEntreTentativasMs, ApplicationBeansConfig::dormir);
    }

    @Bean
    public SolicitarPagamentoPixUseCase solicitarPagamentoPixUseCase(
            FaturaRepositoryPort faturaRepository, PagamentoSagaOrchestrator sagaOrchestrator) {
        return new SolicitarPagamentoPixUseCase(faturaRepository, sagaOrchestrator);
    }

    @Bean
    public ConsultarFaturaUseCase consultarFaturaUseCase(FaturaRepositoryPort faturaRepository) {
        return new ConsultarFaturaUseCase(faturaRepository);
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
