package com.projetocore.payment.infrastructure.persistence;

import com.projetocore.payment.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@org.springframework.context.annotation.Import(FaturaRepositoryAdapter.class)
class FaturaRepositoryAdapterTest {

    @Autowired
    private FaturaRepositoryAdapter repository;

    @Autowired
    private FaturaJpaRepository jpaRepository;

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
    void persisteERecuperaPorId() {
        Fatura fatura = novaFatura("demo-001");

        repository.salvar(fatura);
        Fatura recuperada = repository.buscarPorId(fatura.getId()).orElseThrow();

        assertThat(recuperada.getIdempotencyKey()).isEqualTo("demo-001");
        assertThat(recuperada.getStatus()).isEqualTo(StatusFatura.RECEBIDA);
        assertThat(recuperada.getValorTransacao().valor()).isEqualByComparingTo("23.99");
    }

    @Test
    void recuperaPorIdempotencyKey() {
        Fatura fatura = novaFatura("demo-002");
        repository.salvar(fatura);

        assertThat(repository.buscarPorIdempotencyKey("demo-002")).isPresent();
        assertThat(repository.buscarPorIdempotencyKey("inexistente")).isEmpty();
    }

    @Test
    void constraintUnicaImpedeDuasFaturasComMesmaIdempotencyKey() {
        repository.salvar(novaFatura("demo-003"));
        jpaRepository.flush();

        assertThatThrownBy(() -> {
            repository.salvar(novaFatura("demo-003"));
            jpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
