package com.lelisdev.budget_planner_api.mappers;

import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.models.User;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UserMapper {
 
    UserLoginDto userToUserLoginDto(User user);

}
