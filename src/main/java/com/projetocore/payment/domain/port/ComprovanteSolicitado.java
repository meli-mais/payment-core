package com.projetocore.payment.domain.port;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de uma solicitação de comprovante aceita (202) — espelha
 * {@code identificador_comprovante} + {@code data_hora_requisicao} do contrato.
 */
public record ComprovanteSolicitado(UUID comprovanteId, Instant dataHoraRequisicao) {
}
