package com.projetocore.payment.domain.port;

import com.projetocore.payment.domain.model.ContaBancaria;
import com.projetocore.payment.domain.model.DestinoPix;
import com.projetocore.payment.domain.model.DocumentoIdentificacao;
import com.projetocore.payment.domain.model.ValorTransacao;

import java.time.LocalDateTime;

/**
 * Dados necessários para solicitar a geração de um comprovante — espelha o contrato
 * {@code POST /comprovantes} em specs/001-pix-payment-saga/contracts/comprovantes-consumido.md.
 * Desacoplado da Fatura de propósito: o gateway não precisa (nem deve) conhecer o agregado
 * inteiro, só os dados da transação.
 *
 * <p>{@code dataHoraTransacao} é {@link LocalDateTime}, não {@link java.time.Instant} —
 * o payload do desafio ("2022-04-10T20:03:57.116061100") não traz offset/zona, então tratar
 * como Instant falha ao desserializar (ver research.md).
 */
public record SolicitacaoComprovante(DocumentoIdentificacao documentoOrigem,
                                      ContaBancaria contaOrigem,
                                      ValorTransacao valorTransacao,
                                      DestinoPix destinoPix,
                                      String identificacaoPix,
                                      LocalDateTime dataHoraTransacao) {
}
