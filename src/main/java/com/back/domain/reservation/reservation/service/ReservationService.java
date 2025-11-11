package com.back.domain.reservation.reservation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.dto.CreateReservationReqBody;
import com.back.domain.reservation.reservation.dto.GuestReservationSummaryResBody;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PostService postService;

    public Reservation create(CreateReservationReqBody reqBody, Member author) {
         Post post = postService.getById(reqBody.postId());

        // 1. 기간 중복 체크
        validateNoOverlappingReservation(
                post.getId(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt()
        );

        // 2. 같은 게스트의 중복 예약 체크 (게시글 ID 필요)
        validateNoDuplicateReservation(post.getId(), author.getId());

        // 3. 전달 방식 유효성 체크
        validateDeliveryMethods(post, reqBody);

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .receiveMethod(reqBody.receiveMethod())
                .receiveCarrier(reqBody.receiveCarrier())
                .receiveTrackingNumber(reqBody.receiveTrackingNumber())
                .receiveAddress1(reqBody.receiveAddress1())
                .receiveAddress2(reqBody.receiveAddress2())
                .returnMethod(reqBody.returnMethod())
                .returnCarrier(reqBody.returnCarrier())
                .returnTrackingNumber(reqBody.returnTrackingNumber())
                .reservationStartAt(reqBody.reservationStartAt())
                .reservationEndAt(reqBody.reservationEndAt())
                .author(author)
                .post(post)
                .build();
        return reservationRepository.save(reservation);
    }

    // 기간 중복 체크
    private void validateNoOverlappingReservation(
            Long postId,
            LocalDate startAt,
            LocalDate endAt
    ) {
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
                postId, startAt, endAt
        );

        if (hasOverlap) {
            throw new ServiceException("400-1", "해당 기간에 이미 예약이 있습니다.");
        }
    }

    private void validateNoDuplicateReservation(Long postId, Long authorId) {
        boolean exists = reservationRepository.existsByPostIdAndAuthorId(postId, authorId);
        if (exists) {
            throw new ServiceException("400-2", "이미 해당 게시글에 예약이 존재합니다.");
        }
    }

    private void validateDeliveryMethods(Post post, CreateReservationReqBody reqBody) {
        // 1. 수령 방식 (Receive Method) 검증
        if (!isReceiveMethodAllowed(post.getReceiveMethod(), reqBody.receiveMethod())) {
            throw new ServiceException(
                    "400-3",
                    "게시글에서 허용하는 수령 방식(%s)이 아닙니다.".formatted(post.getReceiveMethod().getDescription())
            );
        }

        // 2. 반납 방식 (Return Method) 검증
        if (!isReturnMethodAllowed(post.getReturnMethod(), reqBody.returnMethod())) {
            throw new ServiceException(
                    "400-4",
                    "게시글에서 허용하는 반납 방식(%s)이 아닙니다.".formatted(post.getReturnMethod().getDescription())
            );
        }
    }
    private boolean isReceiveMethodAllowed(ReceiveMethod postMethod, ReservationDeliveryMethod reqMethod) {
        // ANY인 경우, 모든 방식 허용
        if (postMethod == ReceiveMethod.ANY) {
            return true;
        }

        // Enum 이름(문자열)을 비교하여 동일한지 확인
        return postMethod.name().equals(reqMethod.name());
    }
    private boolean isReturnMethodAllowed(ReturnMethod postMethod, ReservationDeliveryMethod reqMethod) {
        // ANY인 경우, 모든 방식 허용
        if (postMethod == ReturnMethod.ANY) {
            return true;
        }

        // Enum 이름(문자열)을 비교하여 동일한지 확인
        return postMethod.name().equals(reqMethod.name());
    }

    public PagePayload<GuestReservationSummaryResBody> getSentReservations(Member author, Pageable pageable, ReservationStatus status, String keyword) {
        // TODO: post의 제목을 keyword로 검색하도록 수정 필요
        // TODO: QueryDsl로 변경 예정
        Page<Reservation> reservationPage;
        if (status == null) {
            reservationPage = reservationRepository.findByAuthor(author, pageable);
        } else {
            reservationPage = reservationRepository.findByAuthorAndStatus(author, status, pageable);
        }

        Page<GuestReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(GuestReservationSummaryResBody::new);

        return PageUt.of(reservationSummaryDtoPage);
    }

//    public PagePayload<ReservationSummaryDto> getReceivedReservations(
//            Post post,
//            Member author,
//            Pageable pageable,
//            ReservationStatus status,
//            String keyword) {
//        // TODO: postId로 게시글 조회 후, 해당 게시글의 호스트와 author 비교 필요
//        Page<Reservation> reservationPage;
//        if (status == null) {
//            reservationPage = reservationRepository.findByPost(post, pageable);
//        } else {
//            reservationPage = reservationRepository.findByPostAndStatus(post, status, pageable);
//        }
//
//        Page<HostReservationSummaryDto> reservationSummaryDtoPage = reservationPage.map(HostReservationSummaryResBody::new);
//
//        return PageUt.of(reservationSummaryDtoPage);
//    }

    public Reservation getById(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow(
                () -> new IllegalArgumentException("해당 예약을 찾을 수 없습니다. id=" + reservationId)
        );
    }
}
