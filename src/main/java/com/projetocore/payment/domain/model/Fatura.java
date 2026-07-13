package com.projetocore.payment.domain.model;

import com.projetocore.payment.domain.event.FaturaFalhou;
import com.projetocore.payment.domain.event.FaturaPaga;
import com.projetocore.payment.domain.exception.TransicaoInvalidaException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz: representa uma solicitação de pagamento PIX e seu ciclo de vida na SAGA.
 * Único limite de consistência (constitution.md, Princípio II) — todas as transições de
 * estado passam por aqui, nunca são setadas de fora.
 */
public class Fatura {

    private final UUID id;
    private final String idempotencyKey;
    private StatusFatura status;
    private final DocumentoIdentificacao documentoOrigem;
    private final ContaBancaria contaOrigem;
    private final ValorTransacao valorTransacao;
    private final DestinoPix destinoPix;
    private final String identificacaoPix;
    private final LocalDateTime dataHoraTransacao;
    private UUID comprovanteId;
    private String motivoFalha;
    private int tentativasConfirmacao;
    private final Instant criadaEm;
    private Instant atualizadaEm;
    private final List<Object> eventos = new ArrayList<>();

    private Fatura(UUID id, String idempotencyKey, StatusFatura status,
                    DocumentoIdentificacao documentoOrigem, ContaBancaria contaOrigem,
                    ValorTransacao valorTransacao, DestinoPix destinoPix, String identificacaoPix,
                    LocalDateTime dataHoraTransacao, UUID comprovanteId, String motivoFalha,
                    int tentativasConfirmacao, Instant criadaEm, Instant atualizadaEm) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.documentoOrigem = documentoOrigem;
        this.contaOrigem = contaOrigem;
        this.valorTransacao = valorTransacao;
        this.destinoPix = destinoPix;
        this.identificacaoPix = identificacaoPix;
        this.dataHoraTransacao = dataHoraTransacao;
        this.comprovanteId = comprovanteId;
        this.motivoFalha = motivoFalha;
        this.tentativasConfirmacao = tentativasConfirmacao;
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    /** Cria uma nova Fatura em RECEBIDA — ponto de entrada da saga (FR-002). */
    public static Fatura receber(String idempotencyKey, DocumentoIdentificacao documentoOrigem,
                                  ContaBancaria contaOrigem, ValorTransacao valorTransacao,
                                  DestinoPix destinoPix, String identificacaoPix,
                                  LocalDateTime dataHoraTransacao) {
        Objects.requireNonNull(idempotencyKey, "Idempotency-Key é obrigatório");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key não pode ser vazio");
        }
        Objects.requireNonNull(documentoOrigem, "documentoOrigem é obrigatório");
        Objects.requireNonNull(contaOrigem, "contaOrigem é obrigatório");
        Objects.requireNonNull(valorTransacao, "valorTransacao é obrigatório");
        Objects.requireNonNull(destinoPix, "destinoPix é obrigatório");
        Objects.requireNonNull(dataHoraTransacao, "data_hora_transacao é obrigatório");

        Instant agora = Instant.now();
        return new Fatura(UUID.randomUUID(), idempotencyKey, StatusFatura.RECEBIDA,
                documentoOrigem, contaOrigem, valorTransacao, destinoPix, identificacaoPix,
                dataHoraTransacao, null, null, 0, agora, agora);
    }

    /** Reconstitui uma Fatura já existente a partir da persistência — sem validação de novo. */
    public static Fatura reconstituir(UUID id, String idempotencyKey, StatusFatura status,
                                       DocumentoIdentificacao documentoOrigem, ContaBancaria contaOrigem,
                                       ValorTransacao valorTransacao, DestinoPix destinoPix,
                                       String identificacaoPix, LocalDateTime dataHoraTransacao,
                                       UUID comprovanteId, String motivoFalha,
                                       int tentativasConfirmacao, Instant criadaEm, Instant atualizadaEm) {
        return new Fatura(id, idempotencyKey, status, documentoOrigem, contaOrigem, valorTransacao,
                destinoPix, identificacaoPix, dataHoraTransacao, comprovanteId, motivoFalha,
                tentativasConfirmacao, criadaEm, atualizadaEm);
    }

    /** Passo 1 da saga: Comprovantes aceitou a solicitação (202 + UUID) — FR-003. */
    public void solicitarComprovante(UUID comprovanteId) {
        exigirStatus(StatusFatura.RECEBIDA);
        this.comprovanteId = Objects.requireNonNull(comprovanteId, "comprovanteId é obrigatório");
        this.status = StatusFatura.COMPROVANTE_SOLICITADO;
        this.atualizadaEm = Instant.now();
    }

    /** Registra mais uma tentativa de confirmação (polling do GET) — não muda o status. */
    public void registrarTentativaConfirmacao() {
        exigirStatus(StatusFatura.COMPROVANTE_SOLICITADO);
        this.tentativasConfirmacao++;
        this.atualizadaEm = Instant.now();
    }

    /** Confirmação positiva de persistência do comprovante — único caminho para PAGA (FR-004/FR-009). */
    public void confirmarPagamento() {
        exigirStatus(StatusFatura.COMPROVANTE_SOLICITADO);
        this.status = StatusFatura.PAGA;
        this.atualizadaEm = Instant.now();
        eventos.add(new FaturaPaga(id, comprovanteId, atualizadaEm));
    }

    /** Transição compensatória — FR-005. Válida a partir de RECEBIDA ou COMPROVANTE_SOLICITADO. */
    public void falhar(String motivo) {
        if (status != StatusFatura.RECEBIDA && status != StatusFatura.COMPROVANTE_SOLICITADO) {
            throw new TransicaoInvalidaException(
                    "Não é possível transitar para FALHOU a partir de " + status);
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("motivoFalha é obrigatório para transição para FALHOU");
        }
        this.status = StatusFatura.FALHOU;
        this.motivoFalha = motivo;
        this.atualizadaEm = Instant.now();
        eventos.add(new FaturaFalhou(id, comprovanteId, motivo, atualizadaEm));
    }

    private void exigirStatus(StatusFatura esperado) {
        if (this.status != esperado) {
            throw new TransicaoInvalidaException(
                    "Transição inválida: esperado status " + esperado + ", atual " + this.status);
        }
    }

    /** Drena os eventos de domínio levantados desde a última chamada (padrão DDD). */
    public List<Object> pullEventos() {
        List<Object> copia = List.copyOf(eventos);
        eventos.clear();
        return copia;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public StatusFatura getStatus() {
        return status;
    }

    public DocumentoIdentificacao getDocumentoOrigem() {
        return documentoOrigem;
    }

    public ContaBancaria getContaOrigem() {
        return contaOrigem;
    }

    public ValorTransacao getValorTransacao() {
        return valorTransacao;
    }

    public DestinoPix getDestinoPix() {
        return destinoPix;
    }

    public String getIdentificacaoPix() {
        return identificacaoPix;
    }

    public LocalDateTime getDataHoraTransacao() {
        return dataHoraTransacao;
    }

    public UUID getComprovanteId() {
        return comprovanteId;
    }

    public String getMotivoFalha() {
        return motivoFalha;
    }

    public int getTentativasConfirmacao() {
        return tentativasConfirmacao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}
