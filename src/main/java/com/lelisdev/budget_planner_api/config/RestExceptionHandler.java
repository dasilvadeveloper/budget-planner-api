package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.dtos.ErrorDto;
import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Arrays;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler implements AuthenticationEntryPoint {


    @ExceptionHandler(value = {RuntimeException.class})
    public ResponseEntity<ErrorDto> handleRuntimeException(RuntimeException exception) {
        log.info(exception.getMessage(), exception);

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), ErrorCode.E01));
    }

    @ExceptionHandler(value = {ServletException.class})
    public ResponseEntity<ErrorDto> handleServletException(ServletException exception) {
        log.info(exception.getMessage(), exception);

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), ErrorCode.E01));
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorDto> handleException(Exception exception) {
        log.info(exception.getMessage(), exception);

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), ErrorCode.E01));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorDto> handleAppException(AppException exception) {
        log.info(exception.getMessage(), exception);

        return ResponseEntity.status(exception.getError().getHttpStatus())
                .body(new ErrorDto(exception.getError().getMessage(), exception.getError()));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        System.out.println("commence");
        // Configura a resposta HTTP para 401 (Unauthorized) quando o token expirar
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
    }
}
