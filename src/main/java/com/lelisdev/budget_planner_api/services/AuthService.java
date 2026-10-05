package com.lelisdev.budget_planner_api.services;

import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import com.lelisdev.budget_planner_api.mappers.UserMapper;
import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.CharBuffer;
import java.util.UUID;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    // real hash generated once at startup with the same encoder, so it always costs the same as a real user's hash
    private String dummyHash;

    @PostConstruct
    protected void init() {
        dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public UserLoginDto login(CredentialsDto credentialsDto) {
        // GET the user by username, if it doesn't exist so thrown error and return unauthorized
        User user = userRepository.findByUsername(credentialsDto.username()).orElse(null);

        // if the user is not valid, set a DUMMY HASH to force the Bcrypt validation to make the response time equal for both invalid username/password
        // an inactive user is treated exactly like an unknown user
        boolean canLogin = user != null && user.isActive();
        String hashToCheck = canLogin ? user.getPasswordHash() : dummyHash;

        // validate password hash
        boolean matches = passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), hashToCheck);

        if (!canLogin || !matches) throw new AppException(ErrorCode.E02); // return unauthorized

        // return user login dto
        return userMapper.userToUserLoginDto(user); // return the user

    }

}
