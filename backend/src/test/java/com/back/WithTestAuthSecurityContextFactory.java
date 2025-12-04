package com.back;

import com.back.global.security.SecurityUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithTestAuthSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        SecurityUser testUser = new SecurityUser(
                1L,
                "user@test.com",
                "password",
                "testUser",
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        return context;
    }
}