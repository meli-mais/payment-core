package com.projetocore.payment.domain.exception;

/**
 * Lançada pelo adapter de persistência quando duas requisições concorrentes tentam criar uma
 * Fatura com a mesma {@code Idempotency-Key} ao mesmo tempo (ex.: duplo clique) — a constraint
 * única no banco garante que só uma vence a corrida. O caso de uso trata isso recarregando a
 * fatura vencedora, em vez de propagar um erro (FR-006 / US2 — ver spec.md, Edge Cases).
 */
public class IdempotencyKeyDuplicadaException extends RuntimeException {

    public IdempotencyKeyDuplicadaException(String idempotencyKey, Throwable cause) {
        super("Idempotency-Key '" + idempotencyKey + "' já está sendo processada por outra requisição concorrente", cause);
    }
}
