package com.lelisdev.budget_planner_api.repositories;

import com.lelisdev.budget_planner_api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
}
