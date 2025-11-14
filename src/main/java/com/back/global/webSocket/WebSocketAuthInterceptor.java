package com.back.global.webSocket;

import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.global.exception.ServiceException;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final CookieHelper cookieHelper;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String accessToken = cookieHelper.getCookieValue("accessToken", "");

        if (accessToken.isBlank()) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        var claims = authTokenService.payload(accessToken);
        if (claims == null) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
        }

        long id = ((Number) claims.get("id")).longValue();
        long tokenVer = ((Number) claims.getOrDefault("authVersion", 1)).longValue();
        long serverVer = refreshTokenStore.getAuthVersion(id);

        if (tokenVer != serverVer) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "권한이 변경되었습니다. 다시 로그인해주세요.");
        }

        setAuthentication(claims);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}

    private void setAuthentication(Map<String, Object> claims) {
        long id = ((Number) claims.get("id")).longValue();
        String email = (String) claims.get("email");
        String nickname = (String) claims.get("nickname");

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        SecurityUser principal = new SecurityUser(id, email, "", nickname, authorities);

        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
