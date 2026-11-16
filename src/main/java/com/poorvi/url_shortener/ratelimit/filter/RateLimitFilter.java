
package com.poorvi.url_shortener.ratelimit.filter;

import com.poorvi.url_shortener.ratelimit.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String key;
        int limit;

        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(path)) {

            key = "rate:login:" + request.getRemoteAddr();
            limit = 5;

        } else if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/register".equals(path)) {

            key = "rate:register:" + request.getRemoteAddr();
            limit = 5;

        } else if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/urls".equals(path)) {

            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {

                filterChain.doFilter(request, response);
                return;
            }

            key = "rate:create-url:" + authentication.getName();
            limit = 10;

        } else {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed = rateLimitService.isAllowed(
                key, limit, Duration.ofMinutes(1));

        // Updated 429 response
        if (!allowed) {

            response.setStatus(429);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"status\":429,"
                    + "\"error\":\"Too Many Requests\","
                    + "\"message\":\"Too many requests. Please try again later.\","
                    + "\"path\":\"" + path + "\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
