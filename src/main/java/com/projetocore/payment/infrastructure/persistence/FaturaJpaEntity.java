package com.projetocore.payment.infrastructure.persistence;

import com.projetocore.payment.domain.model.StatusFatura;
import com.projetocore.payment.domain.model.TipoChavePix;
import com.projetocore.payment.domain.model.TipoDocumento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação persistente da Fatura — colunas "achatadas" a partir dos Value Objects do
 * domínio. A constraint única em {@code idempotency_key} é o mecanismo real de idempotência
 * (constitution.md, Princípio IV / FR-006), não apenas checagem em memória.
 */
@Entity
@Table(name = "fatura", uniqueConstraints = @UniqueConstraint(name = "uk_fatura_idempotency_key", columnNames = "idempotency_key"))
public class FaturaJpaEntity {

    @Id
    private UUID id;

    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private StatusFatura status;

    private String documentoNome;
    @Enumerated(EnumType.STRING)
    private TipoDocumento documentoTipo;
    private String documentoNumero;

    private String contaAgencia;
    private String contaNumero;
    private String contaDigitoVerificador;

    private BigDecimal valorTransacao;

    @Enumerated(EnumType.STRING)
    private TipoChavePix chavePixTipo;
    private String chavePixValor;
    private String nomeClienteDestino;

    private String identificacaoPix;
    private LocalDateTime dataHoraTransacao;

    private UUID comprovanteId;
    private String motivoFalha;
    private int tentativasConfirmacao;

    private Instant criadaEm;
    private Instant atualizadaEm;

    protected FaturaJpaEntity() {
        // exigido pelo JPA
    }

    public FaturaJpaEntity(UUID id, String idempotencyKey, StatusFatura status,
                            String documentoNome, TipoDocumento documentoTipo, String documentoNumero,
                            String contaAgencia, String contaNumero, String contaDigitoVerificador,
                            BigDecimal valorTransacao, TipoChavePix chavePixTipo, String chavePixValor,
                            String nomeClienteDestino, String identificacaoPix, LocalDateTime dataHoraTransacao,
                            UUID comprovanteId, String motivoFalha, int tentativasConfirmacao,
                            Instant criadaEm, Instant atualizadaEm) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.documentoNome = documentoNome;
        this.documentoTipo = documentoTipo;
        this.documentoNumero = documentoNumero;
        this.contaAgencia = contaAgencia;
        this.contaNumero = contaNumero;
        this.contaDigitoVerificador = contaDigitoVerificador;
        this.valorTransacao = valorTransacao;
        this.chavePixTipo = chavePixTipo;
        this.chavePixValor = chavePixValor;
        this.nomeClienteDestino = nomeClienteDestino;
        this.identificacaoPix = identificacaoPix;
        this.dataHoraTransacao = dataHoraTransacao;
        this.comprovanteId = comprovanteId;
        this.motivoFalha = motivoFalha;
        this.tentativasConfirmacao = tentativasConfirmacao;
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
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

    public String getDocumentoNome() {
        return documentoNome;
    }

    public TipoDocumento getDocumentoTipo() {
        return documentoTipo;
    }

    public String getDocumentoNumero() {
        return documentoNumero;
    }

    public String getContaAgencia() {
        return contaAgencia;
    }

    public String getContaNumero() {
        return contaNumero;
    }

    public String getContaDigitoVerificador() {
        return contaDigitoVerificador;
    }

    public BigDecimal getValorTransacao() {
        return valorTransacao;
    }

    public TipoChavePix getChavePixTipo() {
        return chavePixTipo;
    }

    public String getChavePixValor() {
        return chavePixValor;
    }

    public String getNomeClienteDestino() {
        return nomeClienteDestino;
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
