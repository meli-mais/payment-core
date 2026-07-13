package com.projetocore.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: garante que todo o contexto Spring sobe (fiação de
 * {@code ApplicationBeansConfig}, Resilience4j, WebClient, JPA) — as fatias
 * {@code @WebMvcTest}/{@code @DataJpaTest} não pegam esse tipo de erro de wiring.
 */
@SpringBootTest
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
