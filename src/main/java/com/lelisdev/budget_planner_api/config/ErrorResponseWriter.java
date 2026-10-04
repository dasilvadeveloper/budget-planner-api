package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Escreve um erro no mesmo formato do ErrorDto para respostas geradas fora dos controllers
 * (filtro JWT e entry point), onde o @ControllerAdvice não chega.
 */
public final class ErrorResponseWriter {

    private ErrorResponseWriter() {
    }

    public static void write(HttpServletResponse response, ErrorCode error) throws IOException {
        response.setStatus(error.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + error.getMessage() + "\",\"code\":\"" + error.name() + "\"}");
    }
}
