package com.back.domain.reservation.common;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    PENDING_APPROVAL("승인 대기", false),
    PENDING_PAYMENT("결제 대기", false),
    PENDING_PICKUP("수령 대기", false),
    SHIPPING("배송 중", false),
    INSPECTING_RENTAL("대여 검수", false),
    RENTING("대여 중", false),
    PENDING_RETURN("반납 대기", false),
    RETURNING("반납 중", false),
    RETURN_COMPLETED("반납 완료", false),
    INSPECTING_RETURN("반납 검수", false),
    PENDING_REFUND("환급 예정", false),
    REFUND_COMPLETED("환급 완료", true),
    LOST_OR_UNRETURNED("미반납/분실", false),
    CLAIMING("청구 진행", false),
    CLAIM_COMPLETED("청구 완료", true),
    REJECTED("승인 거절", false),
    CANCELLED("예약 취소", true);

    private final String description;
    private final boolean isReviewable;

    ReservationStatus(String description, boolean isReviewable) {
        this.description = description;
        this.isReviewable = isReviewable;
    }
}
