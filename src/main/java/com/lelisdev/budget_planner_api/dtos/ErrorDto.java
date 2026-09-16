package com.lelisdev.budget_planner_api.dtos;

import com.lelisdev.budget_planner_api.utils.Error;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {
    String message;
    Error code;
    HttpStatus httpStatus;
}
