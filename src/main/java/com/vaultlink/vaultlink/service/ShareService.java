package com.vaultlink.vaultlink.service;

import com.vaultlink.vaultlink.model.FileEntity;
import com.vaultlink.vaultlink.model.ShareLink;
import com.vaultlink.vaultlink.repository.ShareLinkRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileService fileService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ShareService(ShareLinkRepository shareLinkRepository, FileService fileService) {
        this.shareLinkRepository = shareLinkRepository;
        this.fileService = fileService;
    }

    public ShareLink createShareLink(Long fileId, Long requesterId, long expiryHours) {
        FileEntity file = fileService.getFileById(fileId);
        if (!file.getOwnerId().equals(requesterId)) {
            throw new RuntimeException("Not authorized to share this file");
        }

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

        ShareLink link = new ShareLink(fileId, token, expiresAt);
        return shareLinkRepository.save(link);
    }

    public ShareLink getValidLink(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        if (link.isRevoked()) {
            throw new RuntimeException("This sharing link has been revoked");
        }
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This sharing link has expired");
        }
        return link;
    }

    public void revokeLink(String token, Long requesterId) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        FileEntity file = fileService.getFileById(link.getFileId());
        if (!file.getOwnerId().equals(requesterId)) {
            throw new RuntimeException("Not authorized to revoke this link");
        }
        shareLinkRepository.revoke(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[9];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}