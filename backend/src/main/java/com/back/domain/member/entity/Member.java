package com.back.domain.member.entity;

import com.back.domain.member.common.MemberRole;
import com.back.domain.member.dto.MemberUpdateReqBody;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name")
    private String name;

    @Column(name = "phone_number",unique = true)
    private String phoneNumber;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "nickname",unique = true, nullable = false)
    private String nickname;

    @Column(name="is_banned", nullable = false)
    private boolean isBanned;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Column(name = "profile_img_url")
    private String profileImgUrl;

    public static Member createForJoin(String email, String password, String nickname) {
        return new Member(email, password, null, null, null, null, nickname, false, MemberRole.USER, null);
    }

    public static Member createForAdmin(String email, String password, String nickname) {
        return new Member(email, password, null, null, null, null, nickname, false, MemberRole.ADMIN, null);
    }

    public static Member createForOAuth(String email,String nickname, String profileImgUrl) {
        return new Member(email, "", null, null, null, null, nickname, false, MemberRole.USER, profileImgUrl);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public void updateMember(MemberUpdateReqBody reqBody) {
        this.address1 = reqBody.address1();
        this.address2 = reqBody.address2();
        this.name = reqBody.name();
        this.phoneNumber = reqBody.phoneNumber();
    }

    public void updateProfileImage(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
    }

    public void ban() {
        this.isBanned = true;
    }

    public void unban() {
        this.isBanned = false;
    }
}
