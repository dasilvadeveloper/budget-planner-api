package com.lelisdev.budget_planner_api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    E01("Unknown Error", HttpStatus.INTERNAL_SERVER_ERROR),
    E02("Wrong Username/Password", HttpStatus.UNAUTHORIZED),
    E03("Invalid or expired token", HttpStatus.UNAUTHORIZED),
    E04("Invalid request", HttpStatus.BAD_REQUEST),
    E05("Authentication required", HttpStatus.UNAUTHORIZED),
    E06("Resource not found", HttpStatus.NOT_FOUND),
    E07("Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    E08("Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    E09("Access denied", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus httpStatus;
}
