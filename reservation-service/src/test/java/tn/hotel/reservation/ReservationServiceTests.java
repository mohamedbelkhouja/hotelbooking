package tn.hotel.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/reservations"
})
class ReservationServiceTests {

    @Test
    void testBasic() {
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }
}
