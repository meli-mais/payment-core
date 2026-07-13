package com.projetocore.payment.infrastructure.persistence;

import com.projetocore.payment.domain.exception.IdempotencyKeyDuplicadaException;
import com.projetocore.payment.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica, num contexto Spring real (SEM o comportamento especial de transação
 * compartilhada do {@code @DataJpaTest}, que adiaria o flush até uma chamada explícita — ver
 * {@link FaturaRepositoryAdapterTest}), que {@link FaturaRepositoryAdapter#salvar} lança
 * {@link IdempotencyKeyDuplicadaException} de forma SÍNCRONA na própria chamada, sem precisar
 * de flush manual. É essa garantia que {@code SolicitarPagamentoPixUseCase} depende para
 * tratar a corrida entre requisições concorrentes (ver research.md/tasks.md).
 */
@SpringBootTest
class FaturaRepositoryAdapterConcurrencyTest {

    @Autowired
    private FaturaRepositoryAdapter repository;

    private Fatura novaFatura(String idempotencyKey) {
        return Fatura.receber(
                idempotencyKey,
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse("2026-07-08T20:03:57.116061100")
        );
    }

    @Test
    void segundoSalvarComMesmaIdempotencyKeyLancaExcecaoDeDominioSemPrecisarDeFlushManual() {
        repository.salvar(novaFatura("concurrency-test-001"));

        assertThatThrownBy(() -> repository.salvar(novaFatura("concurrency-test-001")))
                .isInstanceOf(IdempotencyKeyDuplicadaException.class);
    }
}
