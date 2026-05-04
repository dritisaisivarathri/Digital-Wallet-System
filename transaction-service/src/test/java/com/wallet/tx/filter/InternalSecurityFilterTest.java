package com.wallet.tx.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InternalSecurityFilterTest {

    @Test
    void doFilter_AllowsDocsPath() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_AllowsValidToken() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/transactions/history");
        request.addHeader(InternalSecurityFilter.INTERNAL_SECRET_HEADER, InternalSecurityFilter.INTERNAL_SECRET_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_RejectsMissingToken() throws Exception {
        InternalSecurityFilter filter = new InternalSecurityFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/transactions/history");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
