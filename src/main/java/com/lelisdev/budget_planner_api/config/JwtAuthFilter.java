package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.exceptions.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserAuthProvider userAuthProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null) {
            String[] authElements = header.split(" ");

            if (authElements.length == 2 && "Bearer".equals(authElements[0])) {
                try {

                    if ("GET".equals(request.getMethod())) {
                        SecurityContextHolder.getContext().setAuthentication(userAuthProvider.validateToken(authElements[1]));
                    } else {
                        SecurityContextHolder.getContext().setAuthentication(userAuthProvider.validateTokenStrongly(authElements[1]));
                    }

                } catch (AppException e) {
                    SecurityContextHolder.clearContext();
                    writeAuthError(response, e.getError().getHttpStatus(), e.getError().getMessage());
                    return;
                } catch (RuntimeException e) {
                    SecurityContextHolder.clearContext();
                    writeAuthError(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeAuthError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
