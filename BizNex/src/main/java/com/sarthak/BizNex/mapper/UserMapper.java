package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.response.AuthResponse;
import com.sarthak.BizNex.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Map enum role to String automatically; other fields map by name
    UserDto toDto(User user);

    // Ignore sensitive/managed fields when mapping from DTO to entity
    @Mappings({
            @Mapping(target = "userPassword", ignore = true),
            @Mapping(target = "mustChangePassword", ignore = true)
    })
    User toEntity(UserDto userDto);

    // Build AuthResponse from User only for user info; tokens are set elsewhere
    @Mappings({
            @Mapping(target = "username", source = "username"),
            @Mapping(target = "userRole", expression = "java(user.getUserRole() != null ? user.getUserRole().name() : null)"),
            @Mapping(target = "mustChangePassword", source = "mustChangePassword"),
            @Mapping(target = "accessToken", ignore = true),
            @Mapping(target = "refreshToken", ignore = true),
            @Mapping(target = "tokenType", ignore = true),
            @Mapping(target = "expireAt", ignore = true)
    })
    AuthResponse userToUserResponseDto(User user);

}
