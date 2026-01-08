package com.bankingmanagement.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")  // Use H2 in-memory database for tests
class PaymentServiceDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
