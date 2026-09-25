package com.vaultlink.vaultlink.service;

import com.vaultlink.vaultlink.model.FileEntity;
import com.vaultlink.vaultlink.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;

    @Value("${file.storage-path}")
    private String storagePath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "xlsx", "png", "jpg", "jpeg", "zip", "txt"
    );

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public FileEntity uploadFile(MultipartFile multipartFile, Long ownerId) {
        String originalName = multipartFile.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new RuntimeException("Invalid file name");
        }

        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new RuntimeException("File type not allowed: " + extension);
        }

        String storedName = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetPath = Paths.get(storagePath, storedName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(multipartFile.getInputStream(), targetPath);

            FileEntity file = new FileEntity(
                    originalName,
                    storedName,
                    targetPath.toString(),
                    multipartFile.getSize(),
                    multipartFile.getContentType(),
                    ownerId
            );
            return fileRepository.save(file);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public List<FileEntity> getFilesByOwner(Long ownerId) {
        return fileRepository.findByOwnerId(ownerId);
    }

    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public void deleteFile(Long id, Long requesterId) {
        FileEntity file = getFileById(id);
        if (!file.getOwnerId().equals(requesterId)) {
            throw new RuntimeException("Not authorized to delete this file");
        }
        try {
            Files.deleteIfExists(Paths.get(file.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from disk");
        }
        fileRepository.deleteById(id);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new RuntimeException("File has no extension");
        }
        return filename.substring(dotIndex + 1);
    }


    public org.springframework.core.io.Resource loadFileAsResource(String filePath) {
    try {
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not readable");
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to load file: " + e.getMessage());
    }
}

} 

    


    