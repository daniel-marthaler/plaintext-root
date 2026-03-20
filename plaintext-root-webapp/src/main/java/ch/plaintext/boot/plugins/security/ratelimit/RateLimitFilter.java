/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RateLimitFilter implements Filter {

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
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest request) ||
            !(servletResponse instanceof HttpServletResponse response)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String path = request.getRequestURI();

        if (path.startsWith("/api/")) {
            String clientIp = getClientIp(request);
            if (!apiLimiter.tryConsume(clientIp)) {
                log.warn("Rate limit exceeded for API from IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests\"}");
                response.setHeader("Retry-After", "60");
                return;
            }
            response.setHeader("X-RateLimit-Remaining", String.valueOf(apiLimiter.getRemainingRequests(clientIp)));
        }

        if (path.equals("/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = getClientIp(request);
            if (!loginLimiter.tryConsume(clientIp)) {
                response.setStatus(429);
                response.sendRedirect("/login.xhtml?error=rate_limited");
                return;
            }
        }

        if (path.startsWith("/autologin")) {
            String clientIp = getClientIp(request);
            if (!loginLimiter.tryConsume(clientIp)) {
                response.setStatus(429);
                response.sendRedirect("/login.xhtml?error=rate_limited");
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredBuckets() {
        apiLimiter.cleanup();
        loginLimiter.cleanup();
    }
}
