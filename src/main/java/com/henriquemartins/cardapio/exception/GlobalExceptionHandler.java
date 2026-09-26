package com.henriquemartins.cardapio.exception;

import com.henriquemartins.cardapio.dto.ApiError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte qualquer falha em um corpo de erro padronizado ({@link ApiError}). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    /** A externa caiu ou demorou demais: 503, nunca a exception crua. */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternal(ExternalServiceException e) {
        log.warn("Servico externo indisponivel: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Requisicao invalida", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParamValidation(ConstraintViolationException e) {
        List<String> details = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Requisicao invalida", details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST,
                "Parametro obrigatorio ausente: " + e.getParameterName(), null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception e) {
        return build(HttpStatus.BAD_REQUEST,
                "Nao foi possivel interpretar a requisicao. Verifique tipos e formatos (datas em ISO: aaaa-MM-dd).",
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        log.error("Erro inesperado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", null);
    }

    private String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, details));
    }
}
