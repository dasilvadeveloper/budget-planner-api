package com.lelisdev.budget_planner_api.mappers;

import com.lelisdev.budget_planner_api.dtos.UserDto;
import com.lelisdev.budget_planner_api.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserDto userDto);

    UserDto toDto(User user);

    List<UserDto> toDtos(List<User> all);
}
