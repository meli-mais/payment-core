package com.projetocore.payment.domain.model;

import java.util.Objects;

/**
 * Value Object para {@code nome} + {@code tipo_documento} + {@code numero_documento}
 * (titular e documento de origem). Validação de dígito verificador de CPF/CNPJ está fora de
 * escopo desta entrega — ver assumptions em specs/001-pix-payment-saga/spec.md.
 */
public record DocumentoIdentificacao(String nome, TipoDocumento tipo, String numero) {

    public DocumentoIdentificacao {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome é obrigatório");
        }
        Objects.requireNonNull(tipo, "tipo_documento é obrigatório");
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("numero_documento é obrigatório");
        }
    }
}
