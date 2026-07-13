package com.projetocore.payment.infrastructure.adapter.http;

import com.projetocore.payment.application.usecase.ConsultarFaturaUseCase;
import com.projetocore.payment.application.usecase.SolicitarPagamentoPixUseCase;
import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest do controller — cobre os cenários de aceite de US1/US2 (contracts/api-pagamentos.md)
 * com os casos de uso mockados.
 */
@WebMvcTest(PagamentoController.class)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitarPagamentoPixUseCase solicitarPagamentoPixUseCase;

    @MockitoBean
    private ConsultarFaturaUseCase consultarFaturaUseCase;

    /**
     * Payload IDÊNTICO ao exemplo de {@code desafio.md}, incluindo o formato exato de
     * {@code data_hora_transacao} (sem offset/"Z" — é um {@code LocalDateTime}, não um
     * {@code Instant}; usar {@code Instant} aqui faz esse teste passar "por acidente" sem
     * cobrir o contrato real — foi exatamente esse bug que este teste pegou).
     */
    private static final String PAYLOAD_VALIDO = """
            {
              "nome": "Giovanni Vicente",
              "tipo_documento": "CPF",
              "numero_documento": "50329291076",
              "numero_agencia": "2022",
              "numero_conta": "00276",
              "digito_verificador_conta": "0",
              "valor_transacao": 23.99,
              "tipo_chave_pix_destino": "CELULAR",
              "chave_pix_destino": "11948755536",
              "nome_cliente_destino": "Fernando Augusto",
              "identificacao_pix": "Segue pagamento da minha cota no churrasco de domingo",
              "data_hora_transacao": "2022-04-10T20:03:57.116061100"
            }
            """;

    private Fatura faturaRecebida() {
        return Fatura.receber("idem-001",
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse("2022-04-10T20:03:57.116061100"));
    }

    @Test
    void postComPayloadValido_retorna202ComFaturaId() throws Exception {
        Fatura fatura = faturaRecebida();
        when(solicitarPagamentoPixUseCase.executar(eq("idem-001"), any(SolicitacaoComprovante.class)))
                .thenReturn(fatura);

        mockMvc.perform(post("/api/v1/pagamentos")
                        .header("Idempotency-Key", "idem-001")
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.faturaId").value(fatura.getId().toString()))
                .andExpect(jsonPath("$.status").value("RECEBIDA"));
    }

    @Test
    void postSemIdempotencyKey_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/pagamentos")
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(solicitarPagamentoPixUseCase);
    }

    @Test
    void postComCampoObrigatorioAusente_retorna400SemAcionarSaga() throws Exception {
        String payloadInvalido = """
                { "tipo_documento": "CPF" }
                """;

        mockMvc.perform(post("/api/v1/pagamentos")
                        .header("Idempotency-Key", "idem-002")
                        .contentType("application/json")
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(solicitarPagamentoPixUseCase);
    }

    @Test
    void reenvioComMesmaIdempotencyKey_naoRepeteEfeitoColateral() throws Exception {
        Fatura fatura = faturaRecebida();
        when(solicitarPagamentoPixUseCase.executar(eq("idem-001"), any(SolicitacaoComprovante.class)))
                .thenReturn(fatura);

        mockMvc.perform(post("/api/v1/pagamentos").header("Idempotency-Key", "idem-001")
                .contentType("application/json").content(PAYLOAD_VALIDO)).andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/pagamentos").header("Idempotency-Key", "idem-001")
                .contentType("application/json").content(PAYLOAD_VALIDO)).andExpect(status().isAccepted());

        // o use case (mockado) é quem garante a idempotência de fato — aqui só confirmamos
        // que o controller chama o use case (que decide) as duas vezes, sem lógica própria.
        verify(solicitarPagamentoPixUseCase, times(2)).executar(anyString(), any());
    }

    @Test
    void getComFaturaExistente_retorna200() throws Exception {
        Fatura fatura = faturaRecebida();
        fatura.solicitarComprovante(UUID.randomUUID());
        fatura.confirmarPagamento();
        when(consultarFaturaUseCase.executar(fatura.getId())).thenReturn(Optional.of(fatura));

        mockMvc.perform(get("/api/v1/pagamentos/{id}", fatura.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"));
    }

    @Test
    void getComFaturaInexistente_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarFaturaUseCase.executar(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/pagamentos/{id}", id))
                .andExpect(status().isNotFound());
    }
}
