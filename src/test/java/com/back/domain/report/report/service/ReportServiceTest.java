package com.back.domain.report.report.service;

import com.back.IntegrationTestSupport;
import com.back.domain.member.member.entity.Member;
import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.dto.ReportReqBody;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportServiceTest extends IntegrationTestSupport {

    @Autowired EntityManager em;
    @Autowired ReportService reportService;
    @Autowired ReportRepository reportRepository;

    Member reporter;

    @BeforeEach
    void setUp() {
        reporter = Member.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트")
                .address1("서울시 강남구")  // 필수 필드 추가
                .address2("테헤란로")      // 선택적 필드
                .nickname("테스트닉네임")   // 필수 필드 추가
                .phoneNumber("010-1234-5678") // 필수 필드 추가
                .build();
        
        em.persist(reporter);
    }

    @Test
    @DisplayName("Type과 TargetId 검증이 통과되면 정상적으로 신고가 등록된다.")
    void postReport_success() {
        //given
        Member target = Member.builder()
                .email("target@example.com")
                .password("password123")
                .name("타겟")
                .address1("서울시 서초구")
                .address2("반포대로")
                .nickname("타겟닉네임")
                .phoneNumber("010-8765-4321")
                .build();

        em.persist(target);


        ReportReqBody reportReqBody = new ReportReqBody(ReportType.USER, target.getId(), "홍보 목적 게시글 신고");
        Long reporterId = reporter.getId();

        //when
        ReportResBody result = reportService.postReport(reportReqBody, reporterId);
        clearContext();

        //then
        Report report = reportRepository.findById(result.reportId()).orElse(null);

        assertThat(report).isNotNull();
        assertThat(report.getComment()).isEqualTo(reportReqBody.comment());
        assertThat(report.getTargetId()).isEqualTo(target.getId());
        assertThat(report.getReportType()).isEqualTo(reportReqBody.reportType());
        assertThat(report.getMember().getId()).isEqualTo(reporter.getId());
    }

    @Test
    @DisplayName("Type 또는 TargetId가 존재하지 않으면 검증에서 예외가 발생한다.")
    void postReport_validationError() {
        //given
        ReportReqBody reportReqBody = new ReportReqBody(ReportType.USER, 999L, "홍보 목적 게시글 신고");
        Long reporterId = reporter.getId();

        //when
        //then
        assertThatThrownBy(() -> reportService.postReport(reportReqBody, reporterId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("신고자가 존재하지 않으면 예외가 발생한다.")
    void postReport_memberNotfoundError() {
        //given
        Member target = Member.builder()
                .email("target@example.com")
                .password("password123")
                .name("타겟")
                .address1("서울시 서초구")
                .address2("반포대로")
                .nickname("타겟닉네임")
                .phoneNumber("010-8765-4321")
                .build();

        em.persist(target);


        ReportReqBody reportReqBody = new ReportReqBody(ReportType.USER, target.getId(), "홍보 목적 게시글 신고");

        //when
        //then
        assertThatThrownBy(() -> reportService.postReport(reportReqBody, 999L))
                .isInstanceOf(ServiceException.class);
    }

    private void clearContext() {
        em.flush();
        em.clear();
    }
}