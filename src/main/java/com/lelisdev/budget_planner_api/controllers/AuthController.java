package com.lelisdev.budget_planner_api.controllers;

import com.lelisdev.budget_planner_api.config.UserAuthProvider;
import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserDto;
import com.lelisdev.budget_planner_api.services.UserService;
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
    UserService userService;

    @Autowired
    private UserAuthProvider userAuthProvider;

    @PostMapping("api/login")
    public ResponseEntity<UserDto> login(@RequestBody CredentialsDto credentialsDto) {
        System.out.println(credentialsDto);
        UserDto userDto = userService.login(credentialsDto);
        userDto.setToken(userAuthProvider.createToken(userDto));

        return ResponseEntity.ok(userDto);
    }
}
