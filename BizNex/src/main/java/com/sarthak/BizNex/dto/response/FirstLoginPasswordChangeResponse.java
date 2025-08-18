package com.sarthak.BizNex.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FirstLoginPasswordChangeResponse {
    private boolean changed;
    private String message;
}

