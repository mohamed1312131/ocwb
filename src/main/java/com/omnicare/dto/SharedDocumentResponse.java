package com.omnicare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SharedDocumentResponse {
    private Long id;
    private String title;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String shareUrl;
    private Boolean revoked;
    private Boolean expired;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long accessCount;
    private LocalDateTime lastAccessedAt;
}
