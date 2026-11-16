
package com.poorvi.url_shortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poorvi.url_shortener.common.exception.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                403,
                "Forbidden",
                "You do not have permission to access this resource",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
