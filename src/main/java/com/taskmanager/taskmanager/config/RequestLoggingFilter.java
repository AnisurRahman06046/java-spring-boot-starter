package com.taskmanager.taskmanager.config;

import java.io.IOException;
import java.util.UUID;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1) // ensure this filter runs beforeeverything else
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        response.setHeader("X-Request-Id", requestId);

        long startTime = System.currentTimeMillis();

        log.info("→ {} {} | ip={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // TODO: handle exception
            long duration = System.currentTimeMillis() - startTime;
            log.info("← {} {} | status={} | {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            MDC.clear();
        }

    }
}
