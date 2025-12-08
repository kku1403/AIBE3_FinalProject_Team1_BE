package com.back.domain.review.controller;

import com.back.config.TestConfig;
import com.back.domain.category.entity.Category;
import com.back.domain.category.repository.CategoryRepository;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TestConfig.class)
class ReviewAdmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private NotificationService notificationService;

    private Member admin;
    private Member regularUser;
    private Member postAuthor;
    private Member renter;
    private Post post;
    private Category category;
    private Reservation reservation;
    private Review review;

    @BeforeEach
    void setUp() {
        // 관리자 생성
        admin = createMember("admin@test.com", "관리자");

        // 일반 사용자 생성
        regularUser = createMember("user@test.com", "일반회원");

        // 게시글 작성자 생성
        postAuthor = createMember("author@test.com", "작성자");

        // 대여자 생성
        renter = createMember("renter@test.com", "대여자");

        // 카테고리 및 게시글 생성
        category = createCategory("테스트");
        post = createPost(postAuthor, "테스트 게시글", "내용", category, 10000, 50000);

        // 예약 생성
        reservation = createReservation(post, renter, 7, 5);

        // 리뷰 생성
        review = createReview(reservation, 5, 5, 5, "좋은 제품이었습니다");
    }

    // ========== 리뷰 제재 테스트 ==========

    @Test
    @Order(1)
    @DisplayName("리뷰 제재 - 성공")
    void banReview_Success() throws Exception {
        // given
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("리뷰가 제재되었습니다."))
                .andExpect(jsonPath("$.data.id").value(review.getId()))
                .andExpect(jsonPath("$.data.isBanned").value(true));

        // DB 검증
        Review bannedReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(bannedReview.isBanned()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("리뷰 제재 - 실패: 이미 제재된 리뷰")
    void banReview_Fail_AlreadyBanned() throws Exception {
        // given - 리뷰 먼저 제재
        review.ban();
        reviewRepository.save(review);

        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(3)
    @DisplayName("리뷰 제재 - 실패: 존재하지 않는 리뷰")
    void banReview_Fail_NotFound() throws Exception {
        // given
        Long nonExistentId = 99999L;
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", nonExistentId)
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(4)
    @DisplayName("리뷰 제재 - 실패: 관리자 권한 없음")
    void banReview_Fail_Forbidden() throws Exception {
        // given - 일반 사용자
        SecurityUser regularSecurityUser = createSecurityUser(regularUser, "ROLE_USER");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId())
                        .with(authentication(createAuthentication(regularSecurityUser))))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("리뷰 제재 - 실패: 인증 없음")
    void banReview_Fail_Unauthorized() throws Exception {
        // when & then - 인증 정보 없이 요청
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ========== 리뷰 제재 해제 테스트 ==========

    @Test
    @Order(6)
    @DisplayName("리뷰 제재 해제 - 성공")
    void unbanReview_Success() throws Exception {
        // given - 리뷰 먼저 제재
        review.ban();
        reviewRepository.save(review);

        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("리뷰 제재가 해제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(review.getId()))
                .andExpect(jsonPath("$.data.isBanned").value(false));

        // DB 검증
        Review unbannedReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(unbannedReview.isBanned()).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("리뷰 제재 해제 - 실패: 제재되지 않은 리뷰")
    void unbanReview_Fail_NotBanned() throws Exception {
        // given - 제재되지 않은 상태
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(8)
    @DisplayName("리뷰 제재 해제 - 실패: 존재하지 않는 리뷰")
    void unbanReview_Fail_NotFound() throws Exception {
        // given
        Long nonExistentId = 99999L;
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", nonExistentId)
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(9)
    @DisplayName("리뷰 제재 해제 - 실패: 관리자 권한 없음")
    void unbanReview_Fail_Forbidden() throws Exception {
        // given - 리뷰 먼저 제재
        review.ban();
        reviewRepository.save(review);

        // 일반 사용자로 요청
        SecurityUser regularSecurityUser = createSecurityUser(regularUser, "ROLE_USER");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", review.getId())
                        .with(authentication(createAuthentication(regularSecurityUser))))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("리뷰 제재 해제 - 실패: 인증 없음")
    void unbanReview_Fail_Unauthorized() throws Exception {
        // given - 리뷰 먼저 제재
        review.ban();
        reviewRepository.save(review);

        // when & then - 인증 정보 없이 요청
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", review.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ========== 시나리오 테스트 ==========

    @Test
    @Order(11)
    @DisplayName("리뷰 제재 후 해제 - 전체 플로우")
    void banAndUnban_FullFlow() throws Exception {
        // given
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // 1. 제재
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBanned").value(true));

        // 2. 제재 상태 확인
        Review bannedReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(bannedReview.isBanned()).isTrue();

        // 3. 제재 해제
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/unban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBanned").value(false));

        // 4. 제재 해제 상태 확인
        Review unbannedReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(unbannedReview.isBanned()).isFalse();
    }

    @Test
    @Order(12)
    @DisplayName("여러 리뷰 제재 - 관리자 권한")
    void banMultipleReviews_Admin() throws Exception {
        // given - 추가 리뷰 생성
        Member renter2 = createMember("renter2@test.com", "대여자2");
        Reservation reservation2 = createReservation(post, renter2, 10, 8);
        Review review2 = createReview(reservation2, 4, 4, 4, "괜찮았습니다");

        Member renter3 = createMember("renter3@test.com", "대여자3");
        Reservation reservation3 = createReservation(post, renter3, 15, 13);
        Review review3 = createReview(reservation3, 3, 3, 3, "보통이었습니다");

        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when - 모든 리뷰 제재
        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review2.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/adm/reviews/{id}/ban", review3.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andExpect(status().isOk());

        List<Review> allReviews = reviewRepository.findAll();
        assertThat(allReviews).hasSize(3);
        assertThat(allReviews).allMatch(Review::isBanned);
    }

    // ========== Helper Methods ==========

    private Member createMember(String username, String nickname) {
        Member member = Member.createForJoin(username, "1234", nickname);
        return memberRepository.save(member);
    }

    private Category createCategory(String name) {
        Category category = Category.create(name, null);
        return categoryRepository.save(category);
    }

    private Post createPost(Member author, String title, String content, Category category,
                            int deposit, int fee) {
        Post post = Post.of(
                title, content,
                ReceiveMethod.DIRECT, ReturnMethod.DIRECT,
                "서울시 강남구", "테헤란로 123",
                deposit, fee, author, category
        );
        return postRepository.save(post);
    }

    private Reservation createReservation(Post post, Member renter, int startDaysAgo, int endDaysAgo) {
        Reservation reservation = new Reservation(
                ReservationStatus.REFUND_COMPLETED,
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

    private Review createReview(Reservation reservation, int equipScore, int kindScore,
                                int timeScore, String comment) {
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(equipScore, kindScore, timeScore, comment);
        Review review = Review.create(reservation, reqBody);
        return reviewRepository.save(review);
    }

    private SecurityUser createSecurityUser(Member member, String role) {
        return new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getNickname(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private UsernamePasswordAuthenticationToken createAuthentication(SecurityUser securityUser) {
        return new UsernamePasswordAuthenticationToken(
                securityUser,
                securityUser.getPassword(),
                securityUser.getAuthorities()
        );
    }
}