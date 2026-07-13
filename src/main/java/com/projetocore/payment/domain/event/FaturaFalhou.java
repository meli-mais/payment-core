package com.projetocore.payment.domain.event;

import java.time.Instant;
import java.util.UUID;

public record FaturaFalhou(UUID faturaId, UUID comprovanteId, String motivo, Instant ocorridoEm) {
}
