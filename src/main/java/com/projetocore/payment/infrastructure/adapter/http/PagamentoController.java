package com.projetocore.payment.infrastructure.adapter.http;

import com.projetocore.payment.application.usecase.ConsultarFaturaUseCase;
import com.projetocore.payment.application.usecase.SolicitarPagamentoPixUseCase;
import com.projetocore.payment.domain.model.Fatura;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pagamentos")
public class PagamentoController {

    private final SolicitarPagamentoPixUseCase solicitarPagamentoPixUseCase;
    private final ConsultarFaturaUseCase consultarFaturaUseCase;

    public PagamentoController(SolicitarPagamentoPixUseCase solicitarPagamentoPixUseCase,
                                ConsultarFaturaUseCase consultarFaturaUseCase) {
        this.solicitarPagamentoPixUseCase = solicitarPagamentoPixUseCase;
        this.consultarFaturaUseCase = consultarFaturaUseCase;
    }

    @PostMapping
    public ResponseEntity<PagamentoResponse> solicitar(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PagamentoRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Header Idempotency-Key é obrigatório");
        }

        Fatura fatura = solicitarPagamentoPixUseCase.executar(idempotencyKey, request.paraSolicitacao());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(PagamentoResponse.de(fatura));
    }

    @GetMapping("/{faturaId}")
    public ResponseEntity<FaturaResponse> consultar(@PathVariable UUID faturaId) {
        return consultarFaturaUseCase.executar(faturaId)
                .map(FaturaResponse::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
