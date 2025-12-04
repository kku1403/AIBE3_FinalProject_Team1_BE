package com.back.domain.notification.controller;

import com.back.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Sql({
        "/sql/categories.sql",
        "/sql/regions.sql",
        "/sql/members.sql",
        "/sql/posts.sql",
        "/sql/reservations.sql",
        "/sql/reviews.sql",
        "/sql/notifications.sql"
})
@Sql(scripts = "/sql/clean-up.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("SSE 구독 연결 - 상태 코드 및 ContentType 확인")
    @WithUserDetails(value = "user1@example.com")
    void subscribe_shouldReturnSseEmitter() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/subscribe"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
