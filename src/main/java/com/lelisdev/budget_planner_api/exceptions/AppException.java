package com.lelisdev.budget_planner_api.exceptions;

import com.lelisdev.budget_planner_api.utils.Error;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppException extends RuntimeException {

    private final Error error;

}
