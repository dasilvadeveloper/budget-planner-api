package com.lelisdev.budget_planner_api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    E01("Unknown Error", HttpStatus.INTERNAL_SERVER_ERROR),
    E02("Wrong Username/Password", HttpStatus.UNAUTHORIZED),
    E03("Invalid or expired token", HttpStatus.UNAUTHORIZED);

    private final String message;
    private final HttpStatus httpStatus;
}
