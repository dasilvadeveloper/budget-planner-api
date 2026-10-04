package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserAuthProvider userAuthProvider;

    // O login não usa token: um token expirado que o cliente ainda anexe não pode impedir um novo login.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + "/api/login");
    }

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
                    ErrorResponseWriter.write(response, e.getError());
                    return;
                } catch (RuntimeException e) {
                    SecurityContextHolder.clearContext();
                    ErrorResponseWriter.write(response, ErrorCode.E03);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
