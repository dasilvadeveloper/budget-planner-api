package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.dtos.ErrorDto;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Arrays;

@ControllerAdvice
public class RestExceptionHandler implements AuthenticationEntryPoint {


@ExceptionHandler(value = {RuntimeException.class})
    public ResponseEntity<ErrorDto> handleRuntimeException(RuntimeException exception) {
        System.out.println("RuntimeException caught");
        System.out.println(exception.getMessage());
        System.out.println(Arrays.toString(exception.getStackTrace()));

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(value = {ServletException.class})
    public ResponseEntity<ErrorDto> handleServletException(ServletException exception) {
        System.out.println("ServletException caught");
        System.out.println(exception.getMessage());
        System.out.println(Arrays.toString(exception.getStackTrace()));

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorDto> handleException(Exception exception) {
        System.out.println("Exception caught");
        System.out.println(exception.getMessage());
        System.out.println(Arrays.toString(exception.getStackTrace()));

        return ResponseEntity.status(500)
                .body(new ErrorDto(exception.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorDto> handleAppException(AppException exception) {
        System.out.println("AppException caught");
        System.out.println(exception.getError().getMessage());
        System.out.println(exception.getError().getHttpStatus());
        exception.getStackTrace();


        return ResponseEntity.status(exception.getError().getHttpStatus())
                .body(new ErrorDto(exception.getError().getMessage(), exception.getError(), exception.getError().getHttpStatus()));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        System.out.println("commence");
        // Configura a resposta HTTP para 401 (Unauthorized) quando o token expirar
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
    }
}
