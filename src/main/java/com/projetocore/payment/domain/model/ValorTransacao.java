package com.projetocore.payment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object para o campo {@code valor_transacao} do contrato PIX (desafio.md).
 */
public record ValorTransacao(BigDecimal valor) {

    public ValorTransacao {
        Objects.requireNonNull(valor, "valor_transacao é obrigatório");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valor_transacao deve ser maior que zero");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }
}
