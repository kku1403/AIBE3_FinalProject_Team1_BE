package com.back.domain.reservation.reservation.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    private ReservationDeliveryMethod receiveMethod;
    private String receiveCarrier;
    private String receiveTrackingNumber;
    private String receiveAddress1;
    private String receiveAddress2;

    @Enumerated(EnumType.STRING)
    private ReservationDeliveryMethod returnMethod;
    private String returnCarrier;
    private String returnTrackingNumber;

    private String cancelReason;
    private String rejectReason;

    private LocalDate reservationStartAt;
    private LocalDate reservationEndAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;
}
