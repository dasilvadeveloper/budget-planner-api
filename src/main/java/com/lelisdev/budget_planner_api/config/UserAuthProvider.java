package com.lelisdev.budget_planner_api.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.enums.ErrorCode;
import com.lelisdev.budget_planner_api.exceptions.AppException;
import com.lelisdev.budget_planner_api.mappers.UserMapper;
import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class UserAuthProvider {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Value("${security.jwt.token.secret}")
    private String secretKey;

    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.token.secret não está definido. ");
        }
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(UserLoginDto dto) {

        System.out.println("creating new token");

        Date now = new Date();
        Date validity = new Date(now.getTime() + 3_600_000);

        return JWT.create()
                .withIssuer(dto.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("firstName", dto.getFirstName())
                .withClaim("id", dto.getId())
                .withClaim("lastName", dto.getLastName())
                .sign(Algorithm.HMAC256(secretKey));
    }

    public Authentication validateToken(String token){
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decoded = verifier.verify(token);

        UserLoginDto user = UserLoginDto.builder()
                .username(decoded.getIssuer())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .build();

        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }

    public Authentication validateTokenStrongly(String token){
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        JWTVerifier verifier = JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        User user = userRepository.findByUsername(decoded.getIssuer())
                .orElseThrow(()-> new AppException(ErrorCode.E03));

        return new UsernamePasswordAuthenticationToken(userMapper.userToUserLoginDto(user), null, Collections.emptyList());
    }
}
