package com.omnicare.service;

import com.omnicare.dto.SharedDocumentResponse;
import com.omnicare.dto.UpdateSharedDocumentRequest;
import com.omnicare.model.SharedDocument;
import com.omnicare.repository.SharedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedDocumentService {

    private final SharedDocumentRepository repository;
    private final CloudinaryStorageService storageService;

    @Value("${share.redirect-base-url:http://localhost:8080}")
    private String redirectBaseUrl;

    public SharedDocumentResponse create(String title, LocalDateTime expiresAt, MultipartFile file) {
        validateExpiry(expiresAt);
        CloudinaryStorageService.UploadResult upload = storageService.uploadPrivateDocument(file);

        SharedDocument document = new SharedDocument();
        document.setTitle(StringUtils.hasText(title) ? title.trim() : file.getOriginalFilename());
        document.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename()));
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        document.setCloudinaryPublicId(upload.getPublicId());
        document.setCloudinaryResourceType(upload.getResourceType());
        document.setCloudinaryDeliveryType(upload.getDeliveryType());
        document.setExpiresAt(expiresAt);
        document.setRevoked(false);
        document.setAccessCount(0L);
        document.setCreatedAt(LocalDateTime.now());

        return toResponse(repository.save(document));
    }

    public List<SharedDocumentResponse> getAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public SharedDocumentResponse update(Long id, UpdateSharedDocumentRequest request) {
        SharedDocument document = getById(id);
        if (StringUtils.hasText(request.getTitle())) {
            document.setTitle(request.getTitle().trim());
        }
        if (Boolean.TRUE.equals(request.getClearExpiry())) {
            document.setExpiresAt(null);
        } else if (request.getExpiresAt() != null) {
            validateExpiry(request.getExpiresAt());
            document.setExpiresAt(request.getExpiresAt());
        }
        document.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(document));
    }

    public SharedDocumentResponse setRevoked(Long id, boolean revoked) {
        SharedDocument document = getById(id);
        document.setRevoked(revoked);
        document.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(document));
    }

    @Transactional
    public String createAccessUrl(String token) {
        SharedDocument document = repository.findByShareToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared document not found"));

        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(document.getRevoked())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This shared document has been revoked");
        }
        if (document.getExpiresAt() != null && !document.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "This shared document has expired");
        }

        document.setAccessCount(document.getAccessCount() + 1);
        document.setLastAccessedAt(now);
        repository.save(document);

        Instant temporaryExpiry = Instant.now().plusSeconds(300);
        if (document.getExpiresAt() != null) {
            Instant configuredExpiry = document.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
            if (configuredExpiry.isBefore(temporaryExpiry)) {
                temporaryExpiry = configuredExpiry;
            }
        }

        return storageService.createTemporaryDownloadUrl(
            document.getCloudinaryPublicId(),
            document.getCloudinaryResourceType(),
            document.getCloudinaryDeliveryType(),
            temporaryExpiry
        );
    }

    public String getShareUrl(Long id) {
        return buildShareUrl(getById(id).getShareToken());
    }

    private SharedDocument getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared document not found"));
    }

    private void validateExpiry(LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiration must be in the future");
        }
    }

    private SharedDocumentResponse toResponse(SharedDocument document) {
        boolean expired = document.getExpiresAt() != null && !document.getExpiresAt().isAfter(LocalDateTime.now());
        return new SharedDocumentResponse(
            document.getId(),
            document.getTitle(),
            document.getOriginalFilename(),
            document.getContentType(),
            document.getSizeBytes(),
            buildShareUrl(document.getShareToken()),
            document.getRevoked(),
            expired,
            document.getExpiresAt(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getAccessCount(),
            document.getLastAccessedAt()
        );
    }

    private String buildShareUrl(String token) {
        String baseUrl = redirectBaseUrl.replaceAll("/+$", "");
        return baseUrl + "/api/public/shares/" + token;
    }
}
