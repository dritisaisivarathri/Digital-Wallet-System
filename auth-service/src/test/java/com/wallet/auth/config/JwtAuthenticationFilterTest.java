package com.wallet.auth.config;

import com.wallet.auth.service.TokenBlacklistService;
import com.wallet.auth.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter buildFilter(JwtUtil jwtUtil) {
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        when(blacklistService.isBlacklisted(anyString())).thenReturn(false);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "blacklistService", blacklistService);
        return filter;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_AuthenticatesWhenUserIdAndRoleExist() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        doNothing().when(jwtUtil).validateToken("valid-token");
        when(jwtUtil.extractUserId("valid-token")).thenReturn("aacfde7e-3408-448a-8c14-2bf5a1bd0f02");
        when(jwtUtil.extractRole("valid-token")).thenReturn("admin");

        JwtAuthenticationFilter filter = buildFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void doFilterInternal_InvalidToken_ReturnsUnauthorized() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.extractUserId("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        JwtAuthenticationFilter filter = buildFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired token"));
    }

    @Test
    void doFilterInternal_MissingBearerPrefix() throws Exception {
        JwtAuthenticationFilter filter = buildFilter(mock(JwtUtil.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "not-bearer token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void doFilterInternal_NoAuthHeader() throws Exception {
        JwtAuthenticationFilter filter = buildFilter(mock(JwtUtil.class));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void doFilterInternal_MissingUserIdInToken() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        doNothing().when(jwtUtil).validateToken("no-user-token");
        when(jwtUtil.extractUserId("no-user-token")).thenReturn(null);

        JwtAuthenticationFilter filter = buildFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer no-user-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }
}
