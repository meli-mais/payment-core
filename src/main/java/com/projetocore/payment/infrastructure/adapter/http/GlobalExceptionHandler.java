package com.projetocore.payment.infrastructure.adapter.http;

import com.projetocore.payment.domain.exception.IdempotencyKeyDuplicadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tradução de erros de validação/domínio para 400, e "não encontrado" para 404. Sem RFC 7807
 * — não é requisito do desafio, mantido simples de propósito (constitution.md não exige).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        String detalhe = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return corpoErro(HttpStatus.BAD_REQUEST, "Requisição inválida", detalhe);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonMalformado(HttpMessageNotReadableException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, "Corpo da requisição malformado", ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentoInvalido(IllegalArgumentException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    /**
     * Só chega aqui no caso extremo em que {@code SolicitarPagamentoPixUseCase} tenta se
     * recuperar de uma corrida de Idempotency-Key e mesmo assim não encontra a fatura
     * concorrente (ver caso de uso) — defesa em profundidade, não o caminho esperado.
     */
    @ExceptionHandler(IdempotencyKeyDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyKeyDuplicada(IdempotencyKeyDuplicadaException ex) {
        return corpoErro(HttpStatus.CONFLICT, "Conflito de concorrência", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, String titulo, String detalhe) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("titulo", titulo);
        corpo.put("detalhe", detalhe);
        return ResponseEntity.status(status).body(corpo);
    }
}
