package tn.hotel.room;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:postgresql://localhost:5433/chambres"
})
class RoomServiceTests {

    @Test
    void testBasic() {
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }
}
