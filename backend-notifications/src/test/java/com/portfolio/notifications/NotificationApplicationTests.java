package com.portfolio.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test — verifies the notification service context loads. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationApplicationTests {

    @Test
    void contextLoads() {
        // Context load failure causes this test to fail
    }
}
