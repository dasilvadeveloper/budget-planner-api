package com.lelisdev.budget_planner_api.controllers;

import com.lelisdev.budget_planner_api.config.UserAuthProvider;
import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAuthProvider userAuthProvider;

    @PostMapping("api/login")
    public ResponseEntity<UserLoginDto> login(@Valid @RequestBody CredentialsDto credentialsDto) {
        UserLoginDto userDto = authService.login(credentialsDto);
        userDto.setToken(userAuthProvider.createToken(userDto));

        return ResponseEntity.ok(userDto);
    }
}
