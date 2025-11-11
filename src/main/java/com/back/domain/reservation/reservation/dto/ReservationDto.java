package com.back.domain.reservation.reservation.dto;

import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationDto(
        Long reservationId,
        // Long postId,
        ReservationStatus status,
        ReservationDeliveryMethod receiveMethod,
        String receiveCarrier,
        String receiveTrackingNumber,
        String receiveAddress1,
        String receiveAddress2,
        ReservationDeliveryMethod returnMethod,
        String returnCarrier,
        String returnTrackingNumber,
        String cancelReason,
        String rejectReason,
        LocalDate reservationStartAt,
        LocalDate reservationEndAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        // Option option,
        // List<ReservationLog> logs,
        int totalAmount
) {
    public ReservationDto(Reservation reservation) {
        this(
                reservation.getId(),
                // TODO: post 필드 매핑
                reservation.getStatus(),
                reservation.getReceiveMethod(),
                reservation.getReceiveCarrier(),
                reservation.getReceiveTrackingNumber(),
                reservation.getReceiveAddress1(),
                reservation.getReceiveAddress2(),
                reservation.getReturnMethod(),
                reservation.getReturnCarrier(),
                reservation.getReturnTrackingNumber(),
                reservation.getCancelReason(),
                reservation.getRejectReason(),
                reservation.getReservationStartAt(),
                reservation.getReservationEndAt(),
                reservation.getCreatedAt(),
                reservation.getModifiedAt(),
                // TODO: option 필드 매핑
                // TODO: logs 필드 매핑
                // TODO: totalAmount 계산
                0 // 임시 값, 실제 계산 로직으로 대체 필요
        );
    }
}
