package com.projetocore.payment.domain.model;

public enum TipoChavePix {
    CELULAR,
    EMAIL,
    CPF,
    CNPJ,
    // Nome DEVE bater com o enum do Comprovantes (provider que desserializa o payload):
    // br.com.ada.mscomprovantes.domain.enums.TipoChavePix.CHAVE_ALEATORIA. Enviamos via
    // .name() em ComprovanteRequest.de(); divergir aqui causa 400 no Comprovantes.
    CHAVE_ALEATORIA
}
