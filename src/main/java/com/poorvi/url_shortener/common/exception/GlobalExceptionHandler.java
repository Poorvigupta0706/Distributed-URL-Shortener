
package com.poorvi.url_shortener.common.exception;

import com.poorvi.url_shortener.url.exception.ExpiredLinkException;
import com.poorvi.url_shortener.url.exception.InvalidUrlException;
import com.poorvi.url_shortener.url.exception.ShortUrlNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ShortUrlNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ExpiredLinkException.class)
    public ResponseEntity<ApiErrorResponse> handleExpired(
            ExpiredLinkException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.GONE, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidUrl(
            InvalidUrlException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return buildResponse(
                HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request body: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON or date format",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Database integrity conflict: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.CONFLICT,
                "The requested resource conflicts with existing data",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected application error", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request);
    }
}
