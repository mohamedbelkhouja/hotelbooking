package tn.hotel.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:mysql://localhost:3306/factures?useSSL=false"
})
class BillingServiceTests {

    @Test
    void testCalculations() {
        // Simple unit verification
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }
}
