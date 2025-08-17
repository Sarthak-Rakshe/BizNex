package com.sarthak.BizNex.service;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.request.AdminPasswordUpdateRequest;
import com.sarthak.BizNex.dto.request.UserRegistrationRequest;
import com.sarthak.BizNex.dto.response.AdminPasswordUpdateResponse;
import com.sarthak.BizNex.entity.User;

import java.util.List;

public interface UserService {

        User registerUser(UserRegistrationRequest request);

        User getUserByUsername(String username);

        List<UserDto> getAllUsers();

        AdminPasswordUpdateResponse updatePassword(String username, AdminPasswordUpdateRequest request);

        String deleteUserByUsername(String username);
}
