package com.lelisdev.budget_planner_api.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResult {

        private boolean isValid;
        private String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
}
