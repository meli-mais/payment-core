package com.projetocore.payment.application.support;

import com.projetocore.payment.domain.exception.ComprovanteIndisponivelException;
import com.projetocore.payment.domain.port.ComprovanteGatewayPort;
import com.projetocore.payment.domain.port.ComprovanteSolicitado;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;

import java.time.Instant;
import java.util.UUID;
import java.util.function.IntPredicate;

/**
 * Fake configurável de {@link ComprovanteGatewayPort} para testes da saga — simula os
 * cenários de US1 (sucesso), US3 (rejeição no POST, confirmação nunca chega).
 */
public class FakeComprovanteGateway implements ComprovanteGatewayPort {

    private boolean rejeitarSolicitacao = false;
    private IntPredicate confirmaNaTentativa = tentativa -> true; // por padrão, confirma já na 1ª

    private int chamadasSolicitar = 0;
    private int chamadasConfirmar = 0;

    public static FakeComprovanteGateway confirmaImediatamente() {
        return new FakeComprovanteGateway();
    }

    public static FakeComprovanteGateway rejeitaSolicitacao() {
        FakeComprovanteGateway fake = new FakeComprovanteGateway();
        fake.rejeitarSolicitacao = true;
        return fake;
    }

    public static FakeComprovanteGateway nuncaConfirma() {
        FakeComprovanteGateway fake = new FakeComprovanteGateway();
        fake.confirmaNaTentativa = tentativa -> false;
        return fake;
    }

    @Override
    public ComprovanteSolicitado solicitar(SolicitacaoComprovante dados) {
        chamadasSolicitar++;
        if (rejeitarSolicitacao) {
            throw new ComprovanteIndisponivelException(
                    "Comprovantes rejeitou a solicitação (simulado em teste)");
        }
        return new ComprovanteSolicitado(UUID.randomUUID(), Instant.now());
    }

    @Override
    public boolean confirmarPersistencia(UUID comprovanteId) {
        chamadasConfirmar++;
        return confirmaNaTentativa.test(chamadasConfirmar);
    }

    public int chamadasSolicitar() {
        return chamadasSolicitar;
    }

    public int chamadasConfirmar() {
        return chamadasConfirmar;
    }
}
