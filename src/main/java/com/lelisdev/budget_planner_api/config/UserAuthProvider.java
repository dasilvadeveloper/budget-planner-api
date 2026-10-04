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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class UserAuthProvider {

    private static final String ISSUER = "budget-planner-api";
    private static final long TOKEN_VALIDITY_MS = 3_600_000;
    // HS256 precisa de uma chave com pelo menos 256 bits
    private static final int MIN_SECRET_BYTES = 32;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Value("${security.jwt.token.secret}")
    private String secretKey;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.token.secret não está definido. ");
        }

        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.token.secret tem de ter pelo menos " + MIN_SECRET_BYTES + " bytes.");
        }

        algorithm = Algorithm.HMAC256(secretBytes);
        verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    public String createToken(UserLoginDto dto) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_VALIDITY_MS);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(dto.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("firstName", dto.getFirstName())
                .withClaim("id", dto.getId())
                .withClaim("lastName", dto.getLastName())
                .sign(algorithm);
    }

    public Authentication validateToken(String token){
        DecodedJWT decoded = verifier.verify(token);

        UserLoginDto user = UserLoginDto.builder()
                .id(decoded.getClaim("id").asString())
                .username(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .build();

        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }

    public Authentication validateTokenStrongly(String token){
        DecodedJWT decoded = verifier.verify(token);

        User user = userRepository.findByUsername(decoded.getSubject())
                .orElseThrow(()-> new AppException(ErrorCode.E03));

        return new UsernamePasswordAuthenticationToken(userMapper.userToUserLoginDto(user), null, Collections.emptyList());
    }
}
