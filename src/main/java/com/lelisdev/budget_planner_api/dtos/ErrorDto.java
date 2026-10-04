package com.lelisdev.budget_planner_api.dtos;

import com.lelisdev.budget_planner_api.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {
    String message;
    ErrorCode code;
}
