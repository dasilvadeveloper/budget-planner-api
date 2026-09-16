package com.lelisdev.budget_planner_api.services;

import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserDto;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import com.lelisdev.budget_planner_api.mappers.UserMapper;
import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.lelisdev.budget_planner_api.utils.Error;

import java.nio.CharBuffer;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    public UserDto login(CredentialsDto credentialsDto) {
        User user = userRepository.findByUsername(credentialsDto.username())
                .orElseThrow(() -> new AppException(Error.E1000));

        if (passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()),
                user.getPassword())) {
            return userMapper.toDto(user);
        } else {
            throw new AppException(Error.E1001);
        }

    }

}
