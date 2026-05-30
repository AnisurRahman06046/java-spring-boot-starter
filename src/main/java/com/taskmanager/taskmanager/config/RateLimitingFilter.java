package com.taskmanager.taskmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.taskmanager.common.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ObjectMapper objectMapper;

    // One bucket per IP address — stored in memory
    // Key: "IP:path-category" Value: Bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();

        // Get or create the right bucket for this IP + endpoint combo
        Bucket bucket = resolveBucket(ip, path);

        if (bucket.tryConsume(1)) {
            // Request allowed — continue
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded — reject with 429
            log.warn("Rate limit exceeded for ip={} path={}", ip, path);
            sendRateLimitResponse(response, ip);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(waitSeconds));
            log.warn("Rate limit exceeded for ip={} path={}", ip, path);
            sendRateLimitResponse(response, ip);
        }
    }

    // ─── Assign rate limits based on endpoint ─────────────────────────
    private Bucket resolveBucket(String ip, String path) {
        String bucketKey;
        Bandwidth limit;

        if (path.contains("/auth/login")) {
            // Strict: 5 login attempts per minute per IP
            bucketKey = ip + ":login";
            limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(1))
                    .build();

        } else if (path.contains("/auth/register")) {
            // Strict: 3 registrations per minute per IP
            bucketKey = ip + ":register";
            limit = Bandwidth.builder()
                    .capacity(3)
                    .refillIntervally(3, Duration.ofMinutes(1))
                    .build();

        } else if (path.contains("/auth/")) {
            // Other auth endpoints: 10 per minute
            bucketKey = ip + ":auth";
            limit = Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(1))
                    .build();

        } else {
            // General API: 60 requests per minute per IP
            bucketKey = ip + ":api";
            limit = Bandwidth.builder()
                    .capacity(60)
                    .refillIntervally(60, Duration.ofMinutes(1))
                    .build();
        }

        // computeIfAbsent is thread-safe — creates bucket only if not exists
        return buckets.computeIfAbsent(bucketKey,
                k -> Bucket.builder().addLimit(limit).build());
    }

    // ─── Extract real IP — handles proxies/load balancers ─────────────
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be "client, proxy1, proxy2" — take first
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    // ─── Return proper 429 response ───────────────────────────────────
    private void sendRateLimitResponse(HttpServletResponse response,
            String ip) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = ApiError.of(
                429,
                "Too many requests. Please slow down and try again later.");

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}