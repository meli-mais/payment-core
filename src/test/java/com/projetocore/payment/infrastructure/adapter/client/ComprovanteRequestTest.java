package com.projetocore.payment.infrastructure.adapter.client;

import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava os valores de contrato enviados ao Comprovantes no {@code POST /comprovantes}.
 *
 * O nome de cada {@link TipoChavePix} vai no fio via {@code .name()} e é desserializado
 * pelo enum do Comprovantes ({@code br.com.ada.mscomprovantes.domain.enums.TipoChavePix}).
 * Se algum nome divergir (ex.: {@code ALEATORIA} vs {@code CHAVE_ALEATORIA}), o Comprovantes
 * responde 400 e a SAGA falha. Este teste garante que os 5 valores casam com o provider.
 */
class ComprovanteRequestTest {

    private SolicitacaoComprovante dadosCom(TipoChavePix tipoChave) {
        return new SolicitacaoComprovante(
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(tipoChave, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse("2026-07-08T20:03:57.116061100")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "CELULAR,CELULAR",
            "EMAIL,EMAIL",
            "CPF,CPF",
            "CNPJ,CNPJ",
            "CHAVE_ALEATORIA,CHAVE_ALEATORIA"
    })
    void tipoChavePixVaiComONomeAceitoPeloComprovantes(TipoChavePix tipoChave, String valorNoFio) {
        ComprovanteRequest request = ComprovanteRequest.de(dadosCom(tipoChave));

        assertThat(request.tipoChavePixDestino()).isEqualTo(valorNoFio);
    }
}
