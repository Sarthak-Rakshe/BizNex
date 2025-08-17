package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.response.AuthResponse;
import com.sarthak.BizNex.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    UserDto toDto(User user);
    User toEntity(UserDto userDto);

    AuthResponse userToUserResponseDto(User user);

}
