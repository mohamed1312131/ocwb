package com.omnicare.controller;

import com.omnicare.service.SharedDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/public/shares")
@RequiredArgsConstructor
public class PublicSharedDocumentController {

    private final SharedDocumentService sharedDocumentService;

    @GetMapping("/{token}")
    public ResponseEntity<Void> open(@PathVariable String token) {
        String temporaryDownloadUrl = sharedDocumentService.createAccessUrl(token);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(temporaryDownloadUrl))
            .build();
    }
}
