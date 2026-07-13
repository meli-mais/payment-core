package com.projetocore.payment.infrastructure.adapter.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de sucesso (202) de {@code POST /comprovantes}. */
public record ComprovanteResponse(
        @JsonProperty("identificador_comprovante") String identificadorComprovante,
        @JsonProperty("data_hora_requisicao") String dataHoraRequisicao
) {
}
