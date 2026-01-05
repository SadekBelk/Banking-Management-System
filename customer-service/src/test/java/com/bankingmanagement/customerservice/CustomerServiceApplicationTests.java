package com.bankingmanagement.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")  // Use H2 in-memory database for tests
class CustomerServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
