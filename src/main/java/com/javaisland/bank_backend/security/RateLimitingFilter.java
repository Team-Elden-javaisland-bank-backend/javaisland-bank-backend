package com.javaisland.bank_backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = httpRequest.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(10, 1));

        if (!bucket.tryConsume()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Too many requests\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static class TokenBucket {
        private final int capacity;
        private final int tokensPerSecond;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int capacity, int tokensPerSecond) {
            this.capacity = capacity;
            this.tokensPerSecond = tokensPerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            double elapsedSeconds = (double) elapsedNanos / TimeUnit.SECONDS.toNanos(1);
            tokens = Math.min(capacity, tokens + elapsedSeconds * tokensPerSecond);
            lastRefillNanos = now;
        }
    }
}
