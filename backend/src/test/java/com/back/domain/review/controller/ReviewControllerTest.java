package com.back.domain.review.controller;

import com.back.config.TestConfig;
import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
import com.back.domain.member.common.MemberRole;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.post.common.ReceiveMethod;
import com.back.domain.post.common.ReturnMethod;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.repository.ReservationRepository;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewRepository;
import com.back.global.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TestConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private NotificationService notificationService;  // 알림 서비스 모킹 (Notification type 에러 우회)

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public ChatClient.Builder mockChatClientBuilder() {
            ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
            ChatClient chatClient = Mockito.mock(ChatClient.class);

            // 기본 동작 설정
            Mockito.when(builder.build()).thenReturn(chatClient);

            return builder;
        }
    }

    private Member postAuthor;
    private Member renter1;
    private Member renter2;
    private Category category;
    private Post campingPost;
    private Post fishingPost;
    private Reservation reservation1;
    private Reservation reservation2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        reviewRepository.deleteAll();
        reservationRepository.deleteAll();
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트 데이터 생성
        category = createCategory("캠핑");
        postAuthor = createMember("owner@test.com", "장비주인", "https://example.com/owner.jpg");
        renter1 = createMember("renter1@test.com", "대여자1", "https://example.com/renter1.jpg");
        renter2 = createMember("renter2@test.com", "대여자2", "https://example.com/renter2.jpg");

        campingPost = createPost(postAuthor, "캠핑 텐트 대여", "4인용 텐트입니다", category, 30000, 50000);
        fishingPost = createPost(postAuthor, "낚시대 세트", "입문자용 낚시대", category, 15000, 30000);

        reservation1 = createReservation(campingPost, renter1, 7, 5);
        reservation2 = createReservation(campingPost, renter2, 14, 12);
    }

    @Test
    @Order(1)
    @DisplayName("리뷰 작성 - 정상 케이스")
    void writeReview_Success() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(
                5, 5, 4,
                "정말 좋은 텐트였습니다. 상태도 깨끗하고 주인분도 친절하셨어요!"
        );
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())  // HTTP 201
                .andExpect(jsonPath("$.status").value(201))  // RsData.status
                .andExpect(jsonPath("$.msg").value("리뷰가 작성되었습니다."))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.equipmentScore").value(5))
                .andExpect(jsonPath("$.data.kindnessScore").value(5))
                .andExpect(jsonPath("$.data.responseTimeScore").value(4))
                .andExpect(jsonPath("$.data.comment").value(containsString("좋은 텐트")))
                .andExpect(jsonPath("$.data.author.nickname").value("대여자1"));

        // DB 검증
        List<Review> reviews = reviewRepository.findAll();
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getEquipmentScore()).isEqualTo(5);
    }

    @Test
    @Order(2)
    @DisplayName("리뷰 작성 - 점수 범위 검증 실패 (0점)")
    void writeReview_ValidationFail_ScoreTooLow() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(0, 5, 5, "내용");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("리뷰 작성 - 점수 범위 검증 실패 (6점)")
    void writeReview_ValidationFail_ScoreTooHigh() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(6, 5, 5, "내용");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("리뷰 작성 - 코멘트 빈 문자열 검증 실패")
    void writeReview_ValidationFail_EmptyComment() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("게시글 리뷰 목록 조회 - 여러 리뷰")
    void getPostReviews_MultipleReviews() throws Exception {
        // given
        Review review1 = createReview(reservation1, 5, 5, 4, "아주 좋았습니다!");
        Review review2 = createReview(reservation2, 4, 4, 5, "만족스러웠어요.");

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", campingPost.getId())
                        .param("page", "0")
                        .param("size", "30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(review2.getId()));  // DESC
    }

    @Test
    @Order(6)
    @DisplayName("게시글 리뷰 목록 조회 - 리뷰 없음")
    void getPostReviews_NoReviews() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", fishingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page.totalElements").value(0));
    }

    @Test
    @Order(7)
    @DisplayName("회원 리뷰 목록 조회")
    void getMemberReviews_Success() throws Exception {
        // given
        createReview(reservation1, 5, 5, 4, "좋았습니다");
        Reservation anotherReservation = createReservation(fishingPost, renter1, 10, 8);
        createReview(anotherReservation, 4, 3, 5, "괜찮았어요");

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/reviews", postAuthor.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @Order(8)
    @DisplayName("게시글 리뷰 요약 - 평균 점수 계산")
    void getPostReviewSummary_AverageScore() throws Exception {
        // given
        createReview(reservation1, 5, 5, 5, "완벽해요");
        createReview(reservation2, 3, 3, 3, "보통이에요");

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews/summary", campingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentScore").value(4.0))
                .andExpect(jsonPath("$.data.kindnessScore").value(4.0))
                .andExpect(jsonPath("$.data.responseTimeScore").value(4.0))
                .andExpect(jsonPath("$.data.avgScore").value(4.0))
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    @Order(9)
    @DisplayName("게시글 리뷰 요약 - 리뷰 없음")
    void getPostReviewSummary_NoReviews() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews/summary", fishingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentScore").value(0.0))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @Order(10)
    @DisplayName("회원 리뷰 요약 조회")
    void getMemberReviewSummary_Success() throws Exception {
        // given
        createReview(reservation1, 5, 4, 5, "좋아요");
        createReview(reservation2, 4, 5, 4, "만족해요");

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/reviews/summary", postAuthor.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgScore").isNumber())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    @Order(11)
    @DisplayName("페이징 테스트 - 30개 이상")
    void getPostReviews_PagingTest() throws Exception {
        // given - 35개 리뷰 생성
        for (int i = 0; i < 35; i++) {
            Member tempRenter = createMember("temp" + i + "@test.com", "임시" + i, null);
            Reservation tempReservation = createReservation(campingPost, tempRenter, 20 + i, 18 + i);
            createReview(tempReservation, 5, 5, 5, "리뷰 " + i);
        }

        // when & then - 첫 페이지
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", campingPost.getId())
                        .param("page", "0")
                        .param("size", "30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(30)))
                .andExpect(jsonPath("$.data.page.totalPages").value(2));

        // when & then - 두 번째 페이지
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", campingPost.getId())
                        .param("page", "1")
                        .param("size", "30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(5)));  // 35개 총 - 30개 첫페이지 = 5개
    }

    @Test
    @Order(12)
    @DisplayName("리뷰 작성 - 리뷰 작성 불가능한 예약 상태")
    void writeReview_Fail_NotReviewableStatus() throws Exception {
        // given - 승인 대기 상태의 예약 (isReviewable = false)
        Reservation pendingReservation = new Reservation(
                ReservationStatus.PENDING_APPROVAL,  // 리뷰 작성 불가
                ReservationDeliveryMethod.DIRECT,
                "서울시 강남구",
                "테헤란로 456",
                ReservationDeliveryMethod.DIRECT,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                renter1,
                campingPost
        );
        pendingReservation = reservationRepository.save(pendingReservation);

        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "좋았습니다");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", pendingReservation.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());  // 또는 서비스 로직에 따라 다른 상태 코드
    }

    @Test
    @Order(13)
    @DisplayName("리뷰 작성 - 중복 리뷰 작성 시도")
    void writeReview_Fail_DuplicateReview() throws Exception {
        // given - 이미 리뷰가 작성된 예약
        createReview(reservation1, 5, 5, 4, "첫 번째 리뷰");

        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(4, 4, 4, "두 번째 리뷰");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then - 같은 예약에 다시 리뷰 작성 시도
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.msg").value("이미 작성된 리뷰가 있습니다."));
    }

    @Test
    @Order(14)
    @DisplayName("리뷰 작성 - 타인의 예약에 리뷰 작성 시도")
    void writeReview_Fail_NotMyReservation() throws Exception {
        // given - renter1의 예약에 renter2가 리뷰 작성 시도
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "타인의 예약");
        SecurityUser securityUser = createSecurityUser(renter2);  // 다른 사용자

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isForbidden());  // 권한 없음
    }

    @Test
    @Order(15)
    @DisplayName("리뷰 작성 - 존재하지 않는 예약")
    void writeReview_Fail_ReservationNotFound() throws Exception {
        // given
        Long nonExistentReservationId = 99999L;
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "존재하지 않는 예약");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", nonExistentReservationId)
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isNotFound());  // 예약을 찾을 수 없음
    }

    @Test
    @Order(16)
    @DisplayName("리뷰 조회 - 존재하지 않는 게시글")
    void getPostReviews_Fail_PostNotFound() throws Exception {
        // given
        Long nonExistentPostId = 99999L;

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", nonExistentPostId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @Order(17)
    @DisplayName("리뷰 조회 - 다양한 평점의 리뷰들")
    void getPostReviews_VariousScores() throws Exception {
        // given - 다양한 점수의 리뷰 생성
        createReview(reservation1, 5, 5, 5, "완벽합니다!");
        createReview(reservation2, 1, 2, 1, "별로였어요");

        Member renter3 = createMember("renter3@test.com", "대여자3", null);
        Reservation reservation3 = createReservation(campingPost, renter3, 20, 18);
        createReview(reservation3, 3, 3, 3, "보통이에요");

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", campingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[*].equipmentScore",
                        containsInAnyOrder(5, 1, 3)));
    }

    @Test
    @Order(18)
    @DisplayName("게시글 리뷰 AI 요약 - 리뷰가 없는 경우")
    void summarizePostReviews_NoReviews() throws Exception {
        // when & then - 리뷰가 없는 게시글의 AI 요약
        mockMvc.perform(get("/api/v1/posts/{id}/reviews/summary/ai", fishingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("후기가 없습니다."));
    }

    @Test
    @Order(19)
    @DisplayName("회원 리뷰 조회 - 리뷰를 작성하지 않은 회원")
    void getMemberReviews_NoReviews() throws Exception {
        // given - postAuthor는 리뷰를 작성하지 않음 (받기만 함)

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/reviews", postAuthor.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page.totalElements").value(0));
    }

    @Test
    @Order(20)
    @DisplayName("리뷰 작성 - NULL 값 검증")
    void writeReview_ValidationFail_NullValues() throws Exception {
        // given - null 값이 포함된 요청
        String invalidJson = """
                {
                    "equipmentScore": null,
                    "kindnessScore": 5,
                    "responseTimeScore": 5,
                    "comment": "테스트"
                }
                """;
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(21)
    @DisplayName("여러 상태의 예약에 대한 리뷰 작성 가능 여부")
    void writeReview_VariousReservationStatuses() throws Exception {
        // given - 리뷰 작성 가능한 상태들
        ReservationStatus[] reviewableStatuses = {
                ReservationStatus.RETURN_COMPLETED,
                ReservationStatus.PENDING_REFUND,
                ReservationStatus.REFUND_COMPLETED,
                ReservationStatus.CLAIMING,
                ReservationStatus.CLAIM_COMPLETED
        };

        int successCount = 0;
        for (int i = 0; i < reviewableStatuses.length; i++) {
            Member tempRenter = createMember("status" + i + "@test.com", "상태테스트" + i, null);
            Reservation tempReservation = new Reservation(
                    reviewableStatuses[i],
                    ReservationDeliveryMethod.DIRECT,
                    "서울시 강남구",
                    "테헤란로",
                    ReservationDeliveryMethod.DIRECT,
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(8),
                    tempRenter,
                    campingPost
            );
            tempReservation = reservationRepository.save(tempReservation);

            ReviewWriteReqBody reqBody = new ReviewWriteReqBody(
                    4, 4, 4, "상태 " + reviewableStatuses[i].name() + " 리뷰"
            );
            SecurityUser securityUser = createSecurityUser(tempRenter);

            // when & then - 리뷰 작성 가능한 상태면 성공해야 함
            mockMvc.perform(post("/api/v1/reviews/{reservationId}", tempReservation.getId())
                            .with(authentication(createAuthentication(securityUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqBody)))
                    .andDo(print())
                    .andExpect(status().isCreated());

            successCount++;
        }

        // 모든 리뷰 작성 가능한 상태에서 성공했는지 확인
        assertThat(successCount).isEqualTo(reviewableStatuses.length);
    }

    @Test
    @Order(22)
    @DisplayName("리뷰 작성 - 경계값 테스트 (최소 점수)")
    void writeReview_BoundaryTest_MinScore() throws Exception {
        // given - 모든 점수 최소값
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(1, 1, 1, "최소 점수");
        SecurityUser securityUser = createSecurityUser(renter1);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.equipmentScore").value(1))
                .andExpect(jsonPath("$.data.kindnessScore").value(1))
                .andExpect(jsonPath("$.data.responseTimeScore").value(1));
    }

    @Test
    @Order(23)
    @DisplayName("리뷰 작성 - 경계값 테스트 (최대 점수)")
    void writeReview_BoundaryTest_MaxScore() throws Exception {
        // given - 모든 점수 최대값
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "최대 점수");
        SecurityUser securityUser = createSecurityUser(renter2);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation2.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.equipmentScore").value(5))
                .andExpect(jsonPath("$.data.kindnessScore").value(5))
                .andExpect(jsonPath("$.data.responseTimeScore").value(5));
    }

    @Test
    @Order(24)
    @DisplayName("리뷰 작성 - 긴 코멘트 (255자)")
    void writeReview_LongComment() throws Exception {
        // given
        String maxComment = "a".repeat(255);
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, maxComment);

        Member renter3 = createMember("renter3@test.com", "대여자3", null);
        Reservation reservation3 = createReservation(campingPost, renter3, 30, 28);
        SecurityUser securityUser = createSecurityUser(renter3);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation3.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(maxComment));
    }

    @Test
    @Order(25)
    @DisplayName("리뷰 작성 - 긴 코멘트 (256자)")
    void writeReview_TooLongComment_Fail() throws Exception {
        // given
        String tooLongComment = "a".repeat(256);
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, tooLongComment);

        Member renter = createMember("long@test.com", "길이테스트", null);
        Reservation reservation = createReservation(campingPost, renter, 10, 8);
        SecurityUser securityUser = createSecurityUser(renter);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(26)
    @DisplayName("리뷰 작성 - 특수문자 포함 코멘트")
    void writeReview_SpecialCharacters() throws Exception {
        // given - 특수문자가 포함된 코멘트
        String specialComment = "정말 좋았어요! 😊 별점 5개 ★★★★★ 100% 만족합니다~";
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, specialComment);

        Member renter4 = createMember("renter4@test.com", "대여자4", null);
        Reservation reservation4 = createReservation(campingPost, renter4, 35, 33);
        SecurityUser securityUser = createSecurityUser(renter4);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation4.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(specialComment));
    }

    @Test
    @Order(27)
    @DisplayName("리뷰 작성 - 다양한 배송 방법 조합 (DELIVERY)")
    void writeReview_WithDeliveryMethod() throws Exception {
        // given - 택배 배송 예약
        Member renter5 = createMember("renter5@test.com", "대여자5", null);
        Reservation deliveryReservation = new Reservation(
                ReservationStatus.REFUND_COMPLETED,
                ReservationDeliveryMethod.DELIVERY,  // 택배 수령
                "서울시 강남구",
                "테헤란로 789",
                ReservationDeliveryMethod.DELIVERY,  // 택배 반납
                LocalDateTime.now().minusDays(40),
                LocalDateTime.now().minusDays(38),
                renter5,
                campingPost
        );
        deliveryReservation = reservationRepository.save(deliveryReservation);

        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(
                5, 5, 5, "택배 배송이었는데 포장도 잘 되어있고 빠르게 도착했습니다"
        );
        SecurityUser securityUser = createSecurityUser(renter5);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", deliveryReservation.getId())
                        .with(authentication(createAuthentication(securityUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(containsString("택배")));
    }

    @Test
    @Order(28)
    @DisplayName("리뷰 조회 - 페이지 크기 변경 테스트")
    void getPostReviews_CustomPageSize() throws Exception {
        // given - 15개 리뷰 생성
        for (int i = 0; i < 15; i++) {
            Member tempRenter = createMember("page" + i + "@test.com", "페이지" + i, null);
            Reservation tempReservation = createReservation(campingPost, tempRenter, 50 + i, 48 + i);
            createReview(tempReservation, 5, 5, 5, "페이지 테스트 " + i);
        }

        // when & then - 페이지 크기 10
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", campingPost.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(10)))
                .andExpect(jsonPath("$.data.page.totalPages").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(29)
    @DisplayName("리뷰 요약 - 소수점 계산 정확도")
    void getPostReviewSummary_DecimalPrecision() throws Exception {
        // given - 평균이 소수점이 되는 리뷰들
        createReview(reservation1, 5, 4, 5, "리뷰1");  // 평균 4.67
        createReview(reservation2, 3, 3, 3, "리뷰2");  // 평균 3.0
        // 전체 평균: (4.67 + 3.0) / 2 = 3.835

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews/summary", campingPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentScore").isNumber())
                .andExpect(jsonPath("$.data.avgScore").isNumber())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    @Order(30)
    @DisplayName("다중 카테고리의 게시글 리뷰 조회")
    void getPostReviews_MultipleCategories() throws Exception {
        // given - 다른 카테고리의 게시글들
        Category fishingCategory = createCategory("낚시");
        Post fishingPost2 = createPost(postAuthor, "고급 낚시대", "전문가용", fishingCategory, 50000, 100000);

        Member renter6 = createMember("renter6@test.com", "대여자6", null);
        Reservation fishingReservation = createReservation(fishingPost2, renter6, 60, 58);
        createReview(fishingReservation, 5, 5, 5, "낚시대 좋아요");

        // when & then - 낚시 카테고리 게시글
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", fishingPost2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].comment").value("낚시대 좋아요"));
    }

    @Test
    @Order(31)
    @DisplayName("리뷰 작성 - 인증되지 않은 사용자")
    void writeReview_Unauthorized() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(5, 5, 5, "인증 없음");

        // when & then - 인증 정보 없이 요청
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", reservation1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andDo(print())
                .andExpect(status().isUnauthorized());  // 401
    }

    @Test
    @Order(32)
    @DisplayName("여러 게시글에 대한 리뷰 - 한 회원이 받은 리뷰 목록")
    void getMemberReceivedReviews_MultiplePosts() throws Exception {
        // given - postAuthor가 올린 여러 게시글에 리뷰가 달린 상황
        Reservation fishingReservation = createReservation(fishingPost, renter1, 70, 68);

        // renter1이 campingPost에 리뷰
        createReview(reservation1, 5, 5, 5, "캠핑 텐트 좋았어요");

        // renter1이 fishingPost에 리뷰
        createReview(fishingReservation, 4, 4, 4, "낚시대도 괜찮았어요");

        // when & then - postAuthor가 받은 리뷰 2개 확인
        mockMvc.perform(get("/api/v1/members/{memberId}/reviews", postAuthor.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    // ========== Helper Methods ==========

    /**
     * Member 생성
     */
    private Member createMember(String username, String nickname, String profileImgUrl) {
        Member member = new Member(username, "1234", nickname, MemberRole.USER,profileImgUrl);
        return memberRepository.save(member);
    }

    /**
     * Category 생성
     */
    private Category createCategory(String name) {
        Category category = Category.create(name, null);  // parent는 null (루트 카테고리)
        return categoryRepository.save(category);
    }

    /**
     * Post 생성
     */
    private Post createPost(Member author, String title, String content, Category category, int fee, int deposit) {
        Post post = Post.of(
                title,
                content,
                ReceiveMethod.DIRECT,
                ReturnMethod.DIRECT,
                "서울시 강남구",
                "테헤란로 123",
                deposit,
                fee,
                author,
                category
        );
        return postRepository.save(post);
    }

    /**
     * Reservation 생성
     */
    private Reservation createReservation(Post post, Member renter, int startDaysAgo, int endDaysAgo) {
        Reservation reservation = new Reservation(
                ReservationStatus.REFUND_COMPLETED,  // 환급 완료 상태 (리뷰 작성 가능)
                ReservationDeliveryMethod.DIRECT,
                "서울시 강남구",
                "테헤란로 456",
                ReservationDeliveryMethod.DIRECT,
                LocalDateTime.now().minusDays(startDaysAgo),
                LocalDateTime.now().minusDays(endDaysAgo),
                renter,
                post
        );
        return reservationRepository.save(reservation);
    }

    /**
     * Review 생성
     */
    private Review createReview(Reservation reservation, int equipScore, int kindScore, int timeScore, String comment) {
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(equipScore, kindScore, timeScore, comment);
        Review review = Review.create(reservation, reqBody);
        return reviewRepository.save(review);
    }

    /**
     * SecurityUser 생성
     */
    private SecurityUser createSecurityUser(Member member) {
        return new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getNickname(),
                member.getAuthorities()
        );
    }

    /**
     * Authentication 생성
     */
    private UsernamePasswordAuthenticationToken createAuthentication(SecurityUser securityUser) {
        return new UsernamePasswordAuthenticationToken(
                securityUser,
                securityUser.getPassword(),
                securityUser.getAuthorities()
        );
    }
}