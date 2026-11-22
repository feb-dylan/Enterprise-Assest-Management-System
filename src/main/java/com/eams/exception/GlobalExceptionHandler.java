package com.eams.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One consistent error format for the whole API:
 *
 * { timestamp, status, error, message, path, [messages] }
 *
 * "message" is ALWAYS a string, so the frontend can render it directly.
 * Validation errors additionally include "messages" (field -> message).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------
    // Our own business exceptions
    // ---------------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(
            ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : ex.getStatusCode().toString();
        return build(ex.getStatusCode(), message, request, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Kept for backward compatibility: older code throws
     * IllegalArgumentException for business-rule conflicts.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ---------------------------------------------------------
    // Validation / bad input
    // ---------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        String message = errors.isEmpty()
                ? "Validation failed"
                : errors.values().stream().collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadInput(
            Exception ex, HttpServletRequest request) {

        String message;
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            message = "Invalid value '" + mismatch.getValue()
                    + "' for parameter '" + mismatch.getName() + "'";
        } else if (ex instanceof MissingServletRequestParameterException missing) {
            message = "Missing required parameter '" + missing.getParameterName() + "'";
        } else if (ex instanceof MissingServletRequestPartException missing) {
            message = "Missing required file part '" + missing.getRequestPartName() + "'";
        } else {
            message = "Malformed request body";
        }
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.CONTENT_TOO_LARGE,
                "File is too large", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", request, null);
    }

    // ---------------------------------------------------------
    // Security
    // ---------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED,
                "Invalid email or password", request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "Your account is disabled or inactive", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED,
                "Authentication failed", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action",
                request, null);
    }

    // ---------------------------------------------------------
    // Database
    // ---------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "This operation conflicts with existing data "
                        + "(duplicate value or record still in use)",
                request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "This record was changed by someone else. Please reload and try again.",
                request, null);
    }

    // ---------------------------------------------------------
    // Fallback: never leak internal details
    // ---------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request, null);
    }

    // ---------------------------------------------------------

    public static Map<String, Object> body(
            HttpStatusCode status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        HttpStatus resolved = HttpStatus.resolve(status.value());
        body.put("error", resolved != null
                ? resolved.getReasonPhrase()
                : status.toString());
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        Map<String, Object> body =
                body(status, message, request.getRequestURI());
        if (fieldErrors != null) {
            body.put("messages", fieldErrors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
