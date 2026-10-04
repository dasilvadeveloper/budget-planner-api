package com.lelisdev.budget_planner_api.services;

import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import com.lelisdev.budget_planner_api.mappers.UserMapper;
import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.CharBuffer;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // real hash generated once at startup with the same encoder, so it always costs the same as a real user's hash
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public UserLoginDto login(CredentialsDto credentialsDto) {
        // GET the user by username, if it doesn't exist so thrown error and return unauthorized
        User user = userRepository.findByUsername(credentialsDto.username()).orElse(null);

        // if the user is not valid, set a DUMMY HASH to force the Bcrypt validation to make the response time equal for both invalid username/password
        String hashToCheck = (user != null) ? user.getPassword() : dummyHash;

        // validate password hash
        boolean matches = passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), hashToCheck);

        if (user == null || !matches) throw new AppException(ErrorCode.E02); // return unauthorized

        // return user login dto
        return userMapper.userToUserLoginDto(user); // return the user

    }

}
