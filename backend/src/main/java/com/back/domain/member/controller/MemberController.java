package com.back.domain.member.controller;

import com.back.domain.member.dto.*;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.EmailService;
import com.back.domain.member.service.MemberService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberApi{
    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieHelper cookieHelper;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<RsData<MemberDto>> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        Member member =memberService.join(reqBody);
        MemberDto memberDto = memberService.toMemberDto(member);
        return ResponseEntity.status(201).body(new RsData<>(HttpStatus.CREATED, "회원가입 되었습니다.", memberDto));
    }

    @PostMapping("/login")
    public ResponseEntity<RsData<MemberDto>> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    ) {
        Member member = memberService.authenticateAndGetMember(reqBody.email(), reqBody.password());

        issueTokensAndSetCookies(member);

        MemberDto memberDto = memberService.toMemberDto(member);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "로그인 되었습니다.", memberDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout() {
        revokeRefreshTokenAndClearCookies();

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "로그아웃 되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<RsData<MemberDto>> me(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member member = memberService.getById(securityUser.getId());

        MemberDto memberDto = memberService.toMemberDto(member);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "현재 회원 정보입니다.", memberDto));
    }

    @PatchMapping("/me")
    public ResponseEntity<RsData<MemberDto>> updateMe(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestPart(value = "reqBody") MemberUpdateReqBody reqBody,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        Member member = memberService.updateMember(securityUser.getId(), reqBody, profileImage);

        MemberDto memberDto = memberService.toMemberDto(member);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "회원 정보가 수정되었습니다.", memberDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<SimpleMemberDto>> getMember(
            @PathVariable Long id
    ) {
        Member member = memberService.getById(id);
        SimpleMemberDto memberDto = memberService.toSimpleMemberDto(member);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "회원 정보입니다.", memberDto));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<RsData<MemberNicknameResBody>> checkNickname(
            @RequestParam String nickname
    ){
        boolean isDuplicated = memberService.existsByNickname(nickname);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "닉네임 중복 확인 완료", new MemberNicknameResBody(isDuplicated)));
    }

    @PostMapping("/send-code")
    public ResponseEntity<RsData<MemberSendCodeResBody>> sendVerificationCode(
            @RequestBody @Valid MemberSendCodeReqBody reqBody
    ) {
        LocalDateTime expiresIn = memberService.sendEmailVerificationCode(reqBody.email());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "이메일 인증이 발송되었습니다.", new MemberSendCodeResBody(expiresIn)));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<RsData<MemberVerifyResBody>> verifyCode(
            @RequestBody @Valid MemberVerifyReqBody reqBody
    ) {
        emailService.verifyCode(reqBody.email(), reqBody.code());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "이메일 인증이 완료되었습니다.", new MemberVerifyResBody(true)));
    }

    private void issueTokensAndSetCookies(Member member) {
        String accessToken = authTokenService.genAccessToken(member);
        String refreshToken = authTokenService.issueRefresh(member);

        cookieHelper.setCookie("accessToken", accessToken);
        cookieHelper.setCookie("refreshToken", refreshToken);
    }

    private void revokeRefreshTokenAndClearCookies() {
        String refreshToken = cookieHelper.getCookieValue("refreshToken", null);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revoke(refreshToken);
        }

        cookieHelper.deleteCookie("accessToken");
        cookieHelper.deleteCookie("refreshToken");
    }
}
