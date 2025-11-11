package com.back.domain.report.report.handler;

import com.back.IntegrationTestSupport;
import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.common.validator.DelegatingReportValidator;
import com.back.domain.report.report.common.validator.ReportValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegatingReportValidatorTest extends IntegrationTestSupport {

    @Autowired EntityManager em;
    @Autowired
    DelegatingReportValidator reportValidator;

    Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트")
                .address1("서울시 강남구")  // 필수 필드 추가
                .address2("테헤란로")      // 선택적 필드
                .nickname("테스트닉네임")   // 필수 필드 추가
                .phoneNumber("010-1234-5678") // 필수 필드 추가
                .build();

        em.persist(member);
    }

    @Test
    @DisplayName("검증기 목록에 자기 자신(DelegatingReportValidator)은 제외된다")
    void shouldNotContainSelf() {
        //given
        List<ReportValidator> validators = reportValidator.getReportValidators();

        //when
        boolean containsSelf = validators.stream().anyMatch(validator -> validator instanceof DelegatingReportValidator);

        //then
        assertThat(containsSelf).isFalse();
    }

    @Test
    @DisplayName("신고 타입과 대상이 존재하는 경우 true를 반환한다.")
    void returnTrue() {
        //given

        //when
        boolean result = reportValidator.validateTargetId(ReportType.USER, member.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("신고 대상이 존재하지 않는 경우 예외를 발생시킨다.")
    void throwError() {
        //given

        //when
        //then
        assertThatThrownBy(() -> reportValidator.validateTargetId(ReportType.USER, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 대상: ");
    }
}