package com.lelisdev.budget_planner_api.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum Error {

    E1000("User not found", HttpStatus.UNAUTHORIZED),
    E1001("Wrong password", HttpStatus.UNAUTHORIZED),

    ;

    private final String message;
    private final HttpStatus httpStatus;
}
