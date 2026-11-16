
package com.poorvi.url_shortener.security;

import com.poorvi.url_shortener.ratelimit.filter.RateLimitFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final RateLimitFilter rateLimitFilter;

    // NEW: Custom security error handlers
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtService jwtService,
            RateLimitFilter rateLimitFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {

        this.jwtService = jwtService;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Prevent duplicate servlet-container registration.
    @Bean
    public FilterRegistrationBean<RateLimitFilter>
            rateLimitFilterRegistration() {

        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(rateLimitFilter);
        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // NEW: Standardized 401 and 403 responses
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/test"
                ).permitAll()

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET, "/{shortCode}"
                ).permitAll()

                .requestMatchers(
                    "/api/urls/**"
                ).authenticated()

                .anyRequest().authenticated()
            )

            // JWT authentication first
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class
            )

            // Rate limiting after JWT authentication
            .addFilterAfter(
                rateLimitFilter,
                JwtAuthenticationFilter.class
            );

        return http.build();
    }
}
