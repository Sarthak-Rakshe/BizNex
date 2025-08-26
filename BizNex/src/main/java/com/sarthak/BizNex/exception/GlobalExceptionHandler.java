package com.sarthak.BizNex.exception;

import com.sarthak.BizNex.dto.response.ExceptionResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ExceptionResponseDto> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ExceptionResponseDto.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldError() != null ? ex.getBindingResult().getFieldError().getDefaultMessage() : "Validation failed";
        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handleJpaEntityNotFound(jakarta.persistence.EntityNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponseDto> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponseDto> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponseDto> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionResponseDto> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMostSpecificCause().getMessage(), req);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handleDomainEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ExceptionResponseDto> handleDuplicateEntity(DuplicateEntityException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponseDto> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(CustomerInformationMissingInBillException.class)
    public ResponseEntity<ExceptionResponseDto> handleCustomerInformationMissing(CustomerInformationMissingInBillException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ExceptionResponseDto> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(BillInformationInvalidException.class)
    public ResponseEntity<ExceptionResponseDto> handleBillInformationInvalid(BillInformationInvalidException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidCreditInformationException.class)
    public ResponseEntity<ExceptionResponseDto> handleInvalidCreditInformation(InvalidCreditInformationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ExceptionResponseDto> handleExpiredJwt(ExpiredJwtException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ExceptionResponseDto> handleMalformedJwt(MalformedJwtException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ExceptionResponseDto> handleJwt(JwtException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ExceptionResponseDto> handleWeakPassword(WeakPasswordException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResponseDto> handleGenericRuntime(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseDto> handleUnhandled(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponseDto> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Bad credentials", req);
    }
}
