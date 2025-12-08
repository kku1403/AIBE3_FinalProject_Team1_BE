package com.back.domain.member.controller;

import com.back.config.TestConfig;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.security.SecurityUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
public class MemberAdmControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    private Member admin;
    private Member normalUser;
    private Member bannedTarget;

    @BeforeEach
    void setUp() {
        admin = createMember("admin@test.com", "관리자", true);
        normalUser = createMember("user@test.com", "일반유저", false);
        bannedTarget = createMember("target@test.com", "대상자", false);
    }

    @Test
    @Order(1)
    @DisplayName("회원 제재 - 성공")
    void banMember_Success() throws Exception {
        // given
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/ban", bannedTarget.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("회원이 제재되었습니다."))
                .andExpect(jsonPath("$.data.id").value(bannedTarget.getId()))
                .andExpect(jsonPath("$.data.isBanned").value(true));

        // ✅ DB 검증
        Member updated = memberRepository.findById(bannedTarget.getId()).orElseThrow();
        assertThat(updated.isBanned()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("회원 제재 - 실패: 이미 제재된 회원")
    void banMember_Fail_AlreadyBanned() throws Exception {
        // given
        bannedTarget.ban();
        memberRepository.save(bannedTarget);

        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/ban", bannedTarget.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("이미 차단된 회원입니다."));
    }

    @Test
    @Order(3)
    @DisplayName("회원 제재 - 실패: 존재하지 않는 회원")
    void banMember_Fail_NotFound() throws Exception {
        // given
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/ban", 99999L)
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @Order(4)
    @DisplayName("회원 제재 - 실패: 관리자 권한 없음")
    void banMember_Fail_Forbidden() throws Exception {
        // given
        SecurityUser normalSecurityUser = createSecurityUser(normalUser, "ROLE_USER");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/ban", bannedTarget.getId())
                        .with(authentication(createAuthentication(normalSecurityUser))))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("회원 제재 - 실패: 인증 없음")
    void banMember_Fail_Unauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/adm/members/{id}/ban", bannedTarget.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("회원 제재 해제 - 성공")
    void unbanMember_Success() throws Exception {
        // given
        bannedTarget.ban();
        memberRepository.save(bannedTarget);

        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/unban", bannedTarget.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("회원 제재가 해제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(bannedTarget.getId()))
                .andExpect(jsonPath("$.data.isBanned").value(false));

        // ✅ DB 검증
        Member updated = memberRepository.findById(bannedTarget.getId()).orElseThrow();
        assertThat(updated.isBanned()).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("회원 제재 해제 - 실패: 제재되지 않은 회원")
    void unbanMember_Fail_NotBanned() throws Exception {
        // given
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        // when & then
        mockMvc.perform(patch("/api/v1/adm/members/{id}/unban", bannedTarget.getId())
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("차단되지 않은 회원입니다."));
    }

    @Test
    @Order(8)
    @DisplayName("회원 제재 해제 - 실패: 존재하지 않는 회원")
    void unbanMember_Fail_NotFound() throws Exception {
        SecurityUser adminUser = createSecurityUser(admin, "ROLE_ADMIN");

        mockMvc.perform(patch("/api/v1/adm/members/{id}/unban", 99999L)
                        .with(authentication(createAuthentication(adminUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @Order(9)
    @DisplayName("회원 제재 해제 - 실패: 관리자 권한 없음")
    void unbanMember_Fail_Forbidden() throws Exception {
        bannedTarget.ban();
        memberRepository.save(bannedTarget);

        SecurityUser normalSecurityUser = createSecurityUser(normalUser, "ROLE_USER");

        mockMvc.perform(patch("/api/v1/adm/members/{id}/unban", bannedTarget.getId())
                        .with(authentication(createAuthentication(normalSecurityUser))))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("회원 제재 해제 - 실패: 인증 없음")
    void unbanMember_Fail_Unauthorized() throws Exception {
        bannedTarget.ban();
        memberRepository.save(bannedTarget);

        mockMvc.perform(patch("/api/v1/adm/members/{id}/unban", bannedTarget.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    private Member createMember(String email, String nickname, boolean isAdmin) {
        Member member = isAdmin
                ? Member.createForAdmin(email, "1234", nickname)
                : Member.createForJoin(email, "1234", nickname);
        return memberRepository.save(member);
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
