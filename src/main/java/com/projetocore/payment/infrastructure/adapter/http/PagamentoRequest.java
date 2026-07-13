package com.projetocore.payment.infrastructure.adapter.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request de {@code POST /api/v1/pagamentos} — mesmo shape do contrato de Comprovantes
 * (specs/001-pix-payment-saga/contracts/api-pagamentos.md), já que os dados da transação PIX
 * são os mesmos.
 */
public record PagamentoRequest(
        @JsonProperty("nome") @NotBlank String nome,
        @JsonProperty("tipo_documento") @NotBlank String tipoDocumento,
        @JsonProperty("numero_documento") @NotBlank String numeroDocumento,
        @JsonProperty("numero_agencia") @NotBlank String numeroAgencia,
        @JsonProperty("numero_conta") @NotBlank String numeroConta,
        @JsonProperty("digito_verificador_conta") @NotBlank String digitoVerificadorConta,
        @JsonProperty("valor_transacao") @NotNull @Positive BigDecimal valorTransacao,
        @JsonProperty("tipo_chave_pix_destino") @NotBlank String tipoChavePixDestino,
        @JsonProperty("chave_pix_destino") @NotBlank String chavePixDestino,
        @JsonProperty("nome_cliente_destino") @NotBlank String nomeClienteDestino,
        @JsonProperty("identificacao_pix") String identificacaoPix,
        @JsonProperty("data_hora_transacao") @NotNull LocalDateTime dataHoraTransacao
) {

    /**
     * Converte para o comando de domínio. {@code TipoDocumento.valueOf}/{@code TipoChavePix.valueOf}
     * lançam {@link IllegalArgumentException} para valores fora do enum — tratado como 400 pelo
     * {@link GlobalExceptionHandler}, funcionando como validação adicional "de graça".
     */
    public SolicitacaoComprovante paraSolicitacao() {
        return new SolicitacaoComprovante(
                new DocumentoIdentificacao(nome, TipoDocumento.valueOf(tipoDocumento), numeroDocumento),
                new ContaBancaria(numeroAgencia, numeroConta, digitoVerificadorConta),
                new ValorTransacao(valorTransacao),
                new DestinoPix(new ChavePix(TipoChavePix.valueOf(tipoChavePixDestino), chavePixDestino), nomeClienteDestino),
                identificacaoPix,
                dataHoraTransacao
        );
    }
}
