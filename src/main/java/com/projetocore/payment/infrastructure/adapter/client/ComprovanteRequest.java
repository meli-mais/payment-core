package com.projetocore.payment.infrastructure.adapter.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload de {@code POST /comprovantes}, campos exatamente como documentado em
 * specs/001-pix-payment-saga/contracts/comprovantes-consumido.md (espelha desafio.md).
 */
public record ComprovanteRequest(
        @JsonProperty("nome") String nome,
        @JsonProperty("tipo_documento") String tipoDocumento,
        @JsonProperty("numero_documento") String numeroDocumento,
        @JsonProperty("numero_agencia") String numeroAgencia,
        @JsonProperty("numero_conta") String numeroConta,
        @JsonProperty("digito_verificador_conta") String digitoVerificadorConta,
        @JsonProperty("valor_transacao") BigDecimal valorTransacao,
        @JsonProperty("tipo_chave_pix_destino") String tipoChavePixDestino,
        @JsonProperty("chave_pix_destino") String chavePixDestino,
        @JsonProperty("nome_cliente_destino") String nomeClienteDestino,
        @JsonProperty("identificacao_pix") String identificacaoPix,
        @JsonProperty("data_hora_transacao") LocalDateTime dataHoraTransacao
) {

    public static ComprovanteRequest de(SolicitacaoComprovante dados) {
        return new ComprovanteRequest(
                dados.documentoOrigem().nome(),
                dados.documentoOrigem().tipo().name(),
                dados.documentoOrigem().numero(),
                dados.contaOrigem().agencia(),
                dados.contaOrigem().conta(),
                dados.contaOrigem().digitoVerificador(),
                dados.valorTransacao().valor(),
                dados.destinoPix().chave().tipo().name(),
                dados.destinoPix().chave().valor(),
                dados.destinoPix().nomeClienteDestino(),
                dados.identificacaoPix(),
                dados.dataHoraTransacao()
        );
    }
}
