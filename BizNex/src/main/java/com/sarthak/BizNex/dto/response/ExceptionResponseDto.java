package com.sarthak.BizNex.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionResponseDto {
    private LocalDateTime timestamp; // ISO time of error
    private int status;              // HTTP status code
    private String error;            // Reason phrase / short label
    private String message;          // Human-readable detail
    private String path;             // Request path
    // Optionally future: private String traceId;

    public static ExceptionResponseDto of(int status, String error, String message, String path) {
        return ExceptionResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
