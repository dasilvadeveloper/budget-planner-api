package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.dtos.ErrorDto;
import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler implements AuthenticationEntryPoint {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorDto> handleAppException(AppException exception) {
        ErrorCode error = exception.getError();
        log.info("AppException: {} ({})", error.name(), error.getMessage());

        return ResponseEntity.status(error.getHttpStatus())
                .body(new ErrorDto(error.getMessage(), error));
    }

    // Exceções de segurança lançadas dentro de controllers (ex.: @PreAuthorize) não podem cair no handler genérico como 500.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDto> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity.status(ErrorCode.E05.getHttpStatus())
                .body(new ErrorDto(ErrorCode.E05.getMessage(), ErrorCode.E05));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity.status(ErrorCode.E09.getHttpStatus())
                .body(new ErrorDto(ErrorCode.E09.getMessage(), ErrorCode.E09));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception exception) {
        log.error("Unhandled exception", exception);

        return ResponseEntity.status(ErrorCode.E01.getHttpStatus())
                .body(new ErrorDto(ErrorCode.E01.getMessage(), ErrorCode.E01));
    }

    // Erros do próprio Spring MVC (body inválido, validação, método não suportado, ...):
    // mantém o status que o Spring escolheu, mas responde no formato ErrorDto e sem detalhes internos.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ErrorCode error = switch (statusCode.value()) {
            case 404 -> ErrorCode.E06;
            case 405 -> ErrorCode.E07;
            case 415 -> ErrorCode.E08;
            default -> statusCode.is5xxServerError() ? ErrorCode.E01 : ErrorCode.E04;
        };
        if (statusCode.is5xxServerError()) {
            log.error("Unhandled MVC exception", exception);
        } else {
            // só o tipo: a mensagem pode conter input do cliente
            log.info("Rejected request: {}", exception.getClass().getSimpleName());
        }

        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(new ErrorDto(error.getMessage(), error));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorResponseWriter.write(response, ErrorCode.E05);
    }
}
