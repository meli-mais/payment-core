package com.projetocore.payment.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Nested
    class ValorTransacaoTest {

        @Test
        void aceitaValorPositivoEArredondaParaDuasCasas() {
            var valor = new ValorTransacao(new BigDecimal("23.999"));
            assertThat(valor.valor()).isEqualByComparingTo("24.00");
        }

        @Test
        void rejeitaValorZeroOuNegativo() {
            assertThatThrownBy(() -> new ValorTransacao(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ValorTransacao(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejeitaValorNulo() {
            assertThatThrownBy(() -> new ValorTransacao(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class DocumentoIdentificacaoTest {

        @Test
        void aceitaDocumentoValido() {
            var doc = new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076");
            assertThat(doc.tipo()).isEqualTo(TipoDocumento.CPF);
        }

        @Test
        void rejeitaNumeroVazioOuNulo() {
            assertThatThrownBy(() -> new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, ""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejeitaTipoNulo() {
            assertThatThrownBy(() -> new DocumentoIdentificacao("Giovanni Vicente", null, "50329291076"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejeitaNomeVazioOuNulo() {
            assertThatThrownBy(() -> new DocumentoIdentificacao("", TipoDocumento.CPF, "50329291076"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new DocumentoIdentificacao(null, TipoDocumento.CPF, "50329291076"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ContaBancariaTest {

        @Test
        void aceitaContaValida() {
            var conta = new ContaBancaria("2022", "00276", "0");
            assertThat(conta.agencia()).isEqualTo("2022");
        }

        @Test
        void rejeitaCampoObrigatorioAusente() {
            assertThatThrownBy(() -> new ContaBancaria("", "00276", "0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ContaBancaria("2022", "", "0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ContaBancaria("2022", "00276", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ChavePixTest {

        @Test
        void aceitaChaveValida() {
            var chave = new ChavePix(TipoChavePix.CELULAR, "11948755536");
            assertThat(chave.valor()).isEqualTo("11948755536");
        }

        @Test
        void rejeitaValorVazio() {
            assertThatThrownBy(() -> new ChavePix(TipoChavePix.CELULAR, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class DestinoPixTest {

        @Test
        void aceitaDestinoValido() {
            var destino = new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto");
            assertThat(destino.nomeClienteDestino()).isEqualTo("Fernando Augusto");
        }

        @Test
        void rejeitaNomeClienteAusente() {
            var chave = new ChavePix(TipoChavePix.CELULAR, "11948755536");
            assertThatThrownBy(() -> new DestinoPix(chave, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
