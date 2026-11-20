package com.proyecto.spring.presentation;

import com.proyecto.spring.business.BusinessException;
import com.proyecto.spring.presentation.dto.ApiDtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.*;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final Clock clock;
    public ApiExceptionHandler(Clock clock) { this.clock = clock; }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException ex) {
        return response(ex.getStatus(), ex.getMessage(), ex.getCodigo());
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> invalid(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.joining(", "))
                : "La solicitud contiene datos inválidos";
        return response(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> duplicate(DataIntegrityViolationException ex) {
        return response(HttpStatus.CONFLICT, "El recurso está duplicado o se encuentra en uso", "RN-11");
    }
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> badCredentials(BadCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", "UNAUTHORIZED");
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación", "FORBIDDEN");
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NoResourceFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "Recurso no encontrado", "NOT_FOUND");
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", "METHOD_NOT_ALLOWED");
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", "INTERNAL_ERROR");
    }
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String code) {
        return ResponseEntity.status(status).body(new ErrorResponse(error, code, Instant.now(clock)));
    }
}
