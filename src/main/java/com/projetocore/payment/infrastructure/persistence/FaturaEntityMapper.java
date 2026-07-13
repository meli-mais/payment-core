package com.projetocore.payment.infrastructure.persistence;

import com.projetocore.payment.domain.model.*;

/**
 * Mapper manual entidade JPA ↔ agregado de domínio. Não usa MapStruct aqui porque a
 * conversão envolve "achatar"/reconstruir Value Objects e chamar a factory
 * {@link Fatura#reconstituir}, o que foge do modelo de cópia direta de propriedades do
 * MapStruct — mapeamentos mais simples (DTO HTTP) usam MapStruct normalmente.
 */
public final class FaturaEntityMapper {

    private FaturaEntityMapper() {
    }

    public static FaturaJpaEntity paraEntidade(Fatura fatura) {
        DocumentoIdentificacao documento = fatura.getDocumentoOrigem();
        ContaBancaria conta = fatura.getContaOrigem();
        DestinoPix destino = fatura.getDestinoPix();

        return new FaturaJpaEntity(
                fatura.getId(),
                fatura.getIdempotencyKey(),
                fatura.getStatus(),
                documento.nome(),
                documento.tipo(),
                documento.numero(),
                conta.agencia(),
                conta.conta(),
                conta.digitoVerificador(),
                fatura.getValorTransacao().valor(),
                destino.chave().tipo(),
                destino.chave().valor(),
                destino.nomeClienteDestino(),
                fatura.getIdentificacaoPix(),
                fatura.getDataHoraTransacao(),
                fatura.getComprovanteId(),
                fatura.getMotivoFalha(),
                fatura.getTentativasConfirmacao(),
                fatura.getCriadaEm(),
                fatura.getAtualizadaEm()
        );
    }

    public static Fatura paraDominio(FaturaJpaEntity entidade) {
        return Fatura.reconstituir(
                entidade.getId(),
                entidade.getIdempotencyKey(),
                entidade.getStatus(),
                new DocumentoIdentificacao(entidade.getDocumentoNome(), entidade.getDocumentoTipo(), entidade.getDocumentoNumero()),
                new ContaBancaria(entidade.getContaAgencia(), entidade.getContaNumero(), entidade.getContaDigitoVerificador()),
                new ValorTransacao(entidade.getValorTransacao()),
                new DestinoPix(new ChavePix(entidade.getChavePixTipo(), entidade.getChavePixValor()), entidade.getNomeClienteDestino()),
                entidade.getIdentificacaoPix(),
                entidade.getDataHoraTransacao(),
                entidade.getComprovanteId(),
                entidade.getMotivoFalha(),
                entidade.getTentativasConfirmacao(),
                entidade.getCriadaEm(),
                entidade.getAtualizadaEm()
        );
    }
}
