package com.projetocore.payment.domain.exception;

/**
 * Lançada quando uma transição de estado fora da máquina de estados da Fatura é tentada
 * (ver .specify/memory/constitution.md, seção "Fluxo da SAGA").
 */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String message) {
        super(message);
    }
}
