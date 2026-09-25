package com.vaultlink.vaultlink.controller;

import com.vaultlink.vaultlink.model.FileEntity;
import com.vaultlink.vaultlink.model.ShareLink;
import com.vaultlink.vaultlink.service.FileService;
import com.vaultlink.vaultlink.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShareController {

    private final ShareService shareService;
    private final FileService fileService;

    public ShareController(ShareService shareService, FileService fileService) {
        this.shareService = shareService;
        this.fileService = fileService;
    }

    @PostMapping("/files/{fileId}/share")
    public ShareLink share(@PathVariable Long fileId,
                            @RequestParam(defaultValue = "24") long expiryHours,
                            HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        return shareService.createShareLink(fileId, ownerId, expiryHours);
    }

    @DeleteMapping("/share/{token}")
    public String revoke(@PathVariable String token, HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        shareService.revokeLink(token, ownerId);
        return "Link revoked";
    }

    @GetMapping("/share/{token}/download")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        ShareLink link = shareService.getValidLink(token);
        FileEntity file = fileService.getFileById(link.getFileId());
        Resource resource = fileService.loadFileAsResource(file.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(resource);
    }
}