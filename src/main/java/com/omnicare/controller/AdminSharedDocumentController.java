package com.omnicare.controller;

import com.omnicare.dto.SharedDocumentResponse;
import com.omnicare.dto.UpdateSharedDocumentRequest;
import com.omnicare.service.QrCodeService;
import com.omnicare.service.SharedDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/shared-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSharedDocumentController {

    private final SharedDocumentService sharedDocumentService;
    private final QrCodeService qrCodeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SharedDocumentResponse> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "expiresAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        return ResponseEntity.ok(sharedDocumentService.create(title, expiresAt, file));
    }

    @GetMapping
    public ResponseEntity<List<SharedDocumentResponse>> getAll() {
        return ResponseEntity.ok(sharedDocumentService.getAll());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SharedDocumentResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateSharedDocumentRequest request) {
        return ResponseEntity.ok(sharedDocumentService.update(id, request));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<SharedDocumentResponse> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(sharedDocumentService.setRevoked(id, true));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<SharedDocumentResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(sharedDocumentService.setRevoked(id, false));
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id) {
        byte[] image = qrCodeService.generatePng(sharedDocumentService.getShareUrl(id));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=omnicare-share-" + id + ".png")
            .contentType(MediaType.IMAGE_PNG)
            .body(image);
    }
}
