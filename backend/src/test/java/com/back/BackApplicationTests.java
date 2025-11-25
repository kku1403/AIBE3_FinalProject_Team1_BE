package com.back;

import com.back.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@Import(TestConfig.class)
class BackApplicationTests {

    @Test
    void contextLoads() {
    }

}
