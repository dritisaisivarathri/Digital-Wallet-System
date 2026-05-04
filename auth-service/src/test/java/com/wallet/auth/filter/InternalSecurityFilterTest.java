package com.wallet.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InternalSecurityFilterTest {

    @Test
    void doFilter_ExcludesSwaggerPaths() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_AllowsValidInternalToken() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/some-endpoint");
        request.addHeader(InternalSecurityFilter.INTERNAL_SECRET_HEADER, InternalSecurityFilter.INTERNAL_SECRET_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_BlocksMissingInternalToken() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/some-endpoint");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_BlocksInvalidInternalToken() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/some-endpoint");
        request.addHeader(InternalSecurityFilter.INTERNAL_SECRET_HEADER, "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
