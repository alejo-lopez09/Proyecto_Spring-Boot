package com.proyecto.spring.business;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final String codigo;
    private final HttpStatus status;
    public BusinessException(String error, String codigo, HttpStatus status) {
        super(error); this.codigo = codigo; this.status = status;
    }
    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }
    public static BusinessException conflict(String message, String code) { return new BusinessException(message, code, HttpStatus.CONFLICT); }
    public static BusinessException notFound(String message) { return new BusinessException(message, "NOT_FOUND", HttpStatus.NOT_FOUND); }
    public static BusinessException badRequest(String message) { return new BusinessException(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST); }
    public static BusinessException forbidden(String message) { return new BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN); }
}
