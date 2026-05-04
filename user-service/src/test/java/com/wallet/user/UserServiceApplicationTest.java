package com.wallet.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTest {

    @Test
    void contextLoads() {
        // Test if context loads correctly
    }

    @Test
    void main() {
        // Test if main method runs without issues (basic coverage)
        System.setProperty("spring.profiles.active", "test");
        UserServiceApplication.main(new String[] {});
    }
}
