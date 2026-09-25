package com.vaultlink.vaultlink.controller;

import com.vaultlink.vaultlink.model.FileEntity;
import com.vaultlink.vaultlink.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public FileEntity upload(@RequestParam("file") MultipartFile file,
                              HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        return fileService.uploadFile(file, ownerId);
    }

    @GetMapping
    public List<FileEntity> listFiles(HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        return fileService.getFilesByOwner(ownerId);
    }

    @GetMapping("/{id}")
    public FileEntity getFile(@PathVariable Long id) {
        return fileService.getFileById(id);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewFile(@PathVariable Long id, HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        FileEntity file = fileService.getFileById(id);

        if (!file.getOwnerId().equals(ownerId)) {
            return ResponseEntity.status(403).build();
        }

        Resource resource = fileService.loadFileAsResource(file.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public void deleteFile(@PathVariable Long id, HttpServletRequest request) {
        Long ownerId = (Long) request.getAttribute("userId");
        fileService.deleteFile(id, ownerId);
    }
}