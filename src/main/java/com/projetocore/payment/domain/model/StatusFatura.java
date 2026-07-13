package com.projetocore.payment.domain.model;

/**
 * Máquina de estados da Fatura — ver .specify/memory/constitution.md, seção "Fluxo da SAGA".
 */
public enum StatusFatura {
    RECEBIDA,
    COMPROVANTE_SOLICITADO,
    PAGA,
    FALHOU
}
