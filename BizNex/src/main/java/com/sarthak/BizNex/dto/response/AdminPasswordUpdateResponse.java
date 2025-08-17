package com.sarthak.BizNex.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPasswordUpdateResponse {
    private final String username;
    private final String status;
}
