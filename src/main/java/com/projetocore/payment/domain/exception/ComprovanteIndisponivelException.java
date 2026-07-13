package com.projetocore.payment.domain.exception;

/**
 * Lançada pelo adapter que implementa {@code ComprovanteGatewayPort} quando o serviço de
 * Comprovantes rejeita a solicitação, ou quando a política de resiliência (Resilience4j —
 * ver constitution.md, Princípio IV) se esgota. É o gatilho, do ponto de vista do domínio,
 * para a transição compensatória da saga.
 */
public class ComprovanteIndisponivelException extends RuntimeException {

    public ComprovanteIndisponivelException(String message) {
        super(message);
    }

    public ComprovanteIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
