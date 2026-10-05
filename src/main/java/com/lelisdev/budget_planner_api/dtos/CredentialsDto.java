package com.lelisdev.budget_planner_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

// password: o BCrypt só usa os primeiros 72 bytes, por isso não vale a pena aceitar mais
public record CredentialsDto(@NotBlank @Size(max = 255) String username, @NotEmpty @Size(max = 72) char[] password) {
}
