package com.example.ordermanagement.exception;

import com.example.ordermanagement.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventory(InsufficientInventoryException ex,
                                                                     HttpServletRequest request) {
        log.warn("Insufficient inventory: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_INVENTORY", ex.getMessage(),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex,
                                                                 HttpServletRequest request) {
        log.warn("Invalid order state transition: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ORDER_STATE", ex.getMessage(),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(BusinessValidationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Business validation failed: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BUSINESS_VALIDATION_ERROR", ex.getMessage(),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified by another request. Please retry.", request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "The operation conflicts with existing data (duplicate value or referenced record).",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.debug("Type mismatch: {}", message);
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request.getRequestURI(), null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        log.debug("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", pathOf(request), fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        log.debug("Request rejected ({}): {}", status.value(), ex.getMessage());
        String code = status == HttpStatus.BAD_REQUEST ? "MALFORMED_REQUEST" : status.name();
        return ResponseEntity.status(status).headers(headers)
                .body(errorBody(status, code, status.getReasonPhrase(), pathOf(request), null));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
                                                String path, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(errorBody(status, error, message, path, fieldErrors));
    }

    private ErrorResponse errorBody(HttpStatus status, String error, String message,
                                    String path, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : null;
    }
}
