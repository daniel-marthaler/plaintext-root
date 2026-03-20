/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting filter for REST API endpoints.
 * Limits requests per IP address to prevent abuse.
 *
 * Configure via application.yml:
 *   plaintext.rate-limit.api.max-requests: 60 (default)
 *   plaintext.rate-limit.api.window-seconds: 60 (default)
 *   plaintext.rate-limit.login.max-requests: 10 (default)
 *   plaintext.rate-limit.login.window-seconds: 60 (default)
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter apiLimiter;
    private final RateLimiter loginLimiter;

    public RateLimitFilter(
            @Value("${plaintext.rate-limit.api.max-requests:60}") int apiMaxRequests,
            @Value("${plaintext.rate-limit.api.window-seconds:60}") int apiWindowSeconds,
            @Value("${plaintext.rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${plaintext.rate-limit.login.window-seconds:60}") int loginWindowSeconds) {
        this.apiLimiter = new RateLimiter(apiMaxRequests, apiWindowSeconds * 1000L);
        this.loginLimiter = new RateLimiter(loginMaxRequests, loginWindowSeconds * 1000L);
        log.info("Rate limiting enabled: API={} req/{}s, Login={} req/{}s",
                apiMaxRequests, apiWindowSeconds, loginMaxRequests, loginWindowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Rate limit API endpoints
        if (path.startsWith("/api/")) {
            String clientIp = getClientIp(request);
            if (!apiLimiter.tryConsume(clientIp)) {
                log.warn("Rate limit exceeded for API request from IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                response.setHeader("Retry-After", "60");
                return;
            }
            response.setHeader("X-RateLimit-Remaining",
                    String.valueOf(apiLimiter.getRemainingRequests(clientIp)));
        }

        // Rate limit login attempts
        if (path.equals("/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = getClientIp(request);
            if (!loginLimiter.tryConsume(clientIp)) {
                log.warn("Rate limit exceeded for login attempt from IP: {}", clientIp);
                response.setStatus(429);
                response.sendRedirect("/login.xhtml?error=rate_limited");
                return;
            }
        }

        // Rate limit autologin attempts
        if (path.startsWith("/autologin")) {
            String clientIp = getClientIp(request);
            if (!loginLimiter.tryConsume(clientIp)) {
                log.warn("Rate limit exceeded for autologin attempt from IP: {}", clientIp);
                response.setStatus(429);
                response.sendRedirect("/login.xhtml?error=rate_limited");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract client IP, considering reverse proxy headers.
     */
    String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Periodic cleanup of expired rate limit buckets (every 5 minutes).
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredBuckets() {
        apiLimiter.cleanup();
        loginLimiter.cleanup();
    }
}
