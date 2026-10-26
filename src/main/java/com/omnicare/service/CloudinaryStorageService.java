package com.omnicare.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService {

    private static final long MAX_FILE_SIZE = 50L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "ppt", "pptx", "doc", "docx", "xls", "xlsx", "txt", "zip"
    );

    private final Cloudinary cloudinary;

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    public UploadResult uploadPrivateDocument(MultipartFile file) {
        ensureConfigured();
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String cloudinaryFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "raw",
                    "type", "private",
                    "folder", "omnicare/shared-documents",
                    "public_id", cloudinaryFilename,
                    "overwrite", false
                )
            );

            Object resourceType = result.get("resource_type");
            Object deliveryType = result.get("type");
            return new UploadResult(
                String.valueOf(result.get("public_id")),
                resourceType == null ? "raw" : String.valueOf(resourceType),
                deliveryType == null ? "private" : String.valueOf(deliveryType)
            );
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed", exception);
        }
    }

    public String createTemporaryDownloadUrl(String publicId, String resourceType, String deliveryType, Instant expiresAt) {
        ensureConfigured();
        try {
            return cloudinary.privateDownload(
                publicId,
                null,
                ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", deliveryType,
                    "expires_at", expiresAt.getEpochSecond(),
                    "attachment", false
                )
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create the document access URL", exception);
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(cloudinaryUrl)) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Cloudinary is not configured. Set CLOUDINARY_URL in the backend .env file."
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a non-empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "The maximum file size is 50 MB");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        String extension = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Allowed file types: PDF, PowerPoint, Word, Excel, TXT and ZIP"
            );
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The file must have an extension");
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class UploadResult {
        private String publicId;
        private String resourceType;
        private String deliveryType;
    }
}
