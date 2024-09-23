package com.odin.registrationservice.filters;

import com.odin.registrationservice.utility.CorrelationIdUtil;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

        // If the correlation ID is missing, generate a new one
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = CorrelationIdUtil.generateCorrelationId();
        }

        // Set correlation ID in MDC (for logging) and forward it to downstream systems
        CorrelationIdUtil.setCorrelationId(correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationIdUtil.clear(); // Always clear MDC after the request is processed
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
