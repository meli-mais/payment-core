package com.projetocore.payment.domain.model;

/**
 * Value Object para a conta de origem do pagamento ({@code numero_agencia},
 * {@code numero_conta}, {@code digito_verificador_conta}).
 */
public record ContaBancaria(String agencia, String conta, String digitoVerificador) {

    public ContaBancaria {
        if (isBlank(agencia)) {
            throw new IllegalArgumentException("numero_agencia é obrigatório");
        }
        if (isBlank(conta)) {
            throw new IllegalArgumentException("numero_conta é obrigatório");
        }
        if (isBlank(digitoVerificador)) {
            throw new IllegalArgumentException("digito_verificador_conta é obrigatório");
        }
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
