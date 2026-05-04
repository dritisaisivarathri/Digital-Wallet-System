package com.wallet.auth;

import com.wallet.auth.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.times;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class AuthServiceApplicationTest {

    @Test
    void contextLoads() {
        // Test if context loads correctly
    }

    @Test
    void main() {
        try (MockedStatic<SpringApplication> springApplication = org.mockito.Mockito.mockStatic(SpringApplication.class)) {
            AuthServiceApplication.main(new String[] {});
            springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, new String[] {}), times(1));
        }
    }
}
