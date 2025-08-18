package com.sarthak.BizNex.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirstLoginPasswordChangeRequest {
    @NotBlank
    private String newPassword;
}

