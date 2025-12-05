package com.back.domain.reservation.controller;

import com.back.BaseContainerIntegrationTest;
import com.back.domain.reservation.repository.ReservationRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/sql/reservations.sql")
class ReservationControllerTest extends BaseContainerIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("예약 등록 테스트")
    @WithUserDetails("user3@example.com")
    void createReservationTest() throws Exception {
        LocalDateTime reservationStartAt = LocalDateTime.now().plusDays(30);
        LocalDateTime reservationEndAt = LocalDateTime.now().plusDays(31);

        String startAtStr = reservationStartAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endAtStr = reservationEndAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String reqBody = """
            {
              "receiveMethod": "DIRECT",
              "receiveAddress1": null,
              "receiveAddress2": null,
              "returnMethod": "DIRECT",
              "reservationStartAt": "%s",
              "reservationEndAt": "%s",
              "postId": 5,
              "optionIds": null
            }
            """.formatted(startAtStr, endAtStr);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.status").value(201),
                        jsonPath("$.msg").exists(),
                        jsonPath("$.data.id").exists(),
                        jsonPath("$.data.postId").value(5),
                        jsonPath("$.data.status").value("PENDING_APPROVAL"),
                        jsonPath("$.data.receiveMethod").value("DIRECT"),
                        jsonPath("$.data.returnMethod").value("DIRECT"),
                        jsonPath("$.data.reservationStartAt").value(startAtStr),
                        jsonPath("$.data.reservationEndAt").value(endAtStr),
                        jsonPath("$.data.option").isArray(),
                        jsonPath("$.data.logs").isArray(),
                        jsonPath("$.data.createdAt").exists(),
                        jsonPath("$.data.modifiedAt").exists()
                );
    }
}