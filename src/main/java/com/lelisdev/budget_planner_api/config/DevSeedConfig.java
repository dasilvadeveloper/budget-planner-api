package com.lelisdev.budget_planner_api.config;

import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@Profile("dev")
public class DevSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSeedConfig.class);

    @Bean
    CommandLineRunner seedDevUser(UserRepository userRepository,

                                  PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Já existem utilizadores — seed de dev saltado.");
                return;
            }

            String devPassword = System.getenv("DEV_SEED_PASSWORD");
            if (devPassword == null || devPassword.isBlank()) {
                log.warn("DEV_SEED_PASSWORD não definida — seed de utilizador de dev saltado.");
                log.warn("Define-a no Run Configuration do IntelliJ ou no terminal para criar um user de teste.");
                return;
            }

            User user = new User();
            user.setFirstName("Hendrik");
            user.setLastName("Lelis");
            user.setUsername("hendrik");
            user.setEmail("johndoe@example.com");
            user.setPassword(passwordEncoder.encode(devPassword));
            userRepository.save(user);


            log.info("Utilizador de dev 'hendrik' criado com sucesso.");
        };
    }
}