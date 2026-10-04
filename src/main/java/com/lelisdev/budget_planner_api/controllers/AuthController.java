package com.lelisdev.budget_planner_api.controllers;

import com.lelisdev.budget_planner_api.config.UserAuthProvider;
import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    private UserAuthProvider userAuthProvider;

    @PostMapping("api/login")
    public ResponseEntity<UserLoginDto> login(@RequestBody CredentialsDto credentialsDto) {
        System.out.println(credentialsDto);
        UserLoginDto userDto = authService.login(credentialsDto);
        userDto.setToken(userAuthProvider.createToken(userDto));

        return ResponseEntity.ok(userDto);
    }
}
