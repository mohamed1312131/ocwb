package com.omnicare.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String originalFilename;

    private String contentType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, unique = true, length = 64)
    private String shareToken;

    @Column(nullable = false, unique = true, length = 500)
    private String cloudinaryPublicId;

    @Column(nullable = false, length = 20)
    private String cloudinaryResourceType;

    @Column(nullable = false, length = 20)
    private String cloudinaryDeliveryType;

    @Column(nullable = false)
    private Boolean revoked = false;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accessCount = 0L;

    private LocalDateTime lastAccessedAt;
}
