package com.omnicare.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateSharedDocumentRequest {
    private String title;
    private LocalDateTime expiresAt;
    private Boolean clearExpiry;
}
