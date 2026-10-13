package com.framework.service;

import com.framework.model.CmsFile;
import com.framework.repository.CmsFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final CmsFileRepository cmsFileRepository;
    private final Path uploadDir;
    private final List<String> allowedTypes;

    public FileStorageService(
            CmsFileRepository cmsFileRepository,
            @Value("${app.upload.dir:./uploads}") String uploadDirPath,
            @Value("${app.upload.allowed-types:image/png,image/jpeg,image/gif,image/svg+xml,image/webp}") String allowedTypesStr) {
        this.cmsFileRepository = cmsFileRepository;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.allowedTypes = Arrays.asList(allowedTypesStr.split(","));
    }

    public CmsFile store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        // Create date-based subdirectory
        LocalDate now = LocalDate.now();
        String subDir = now.getYear() + "/" + String.format("%02d", now.getMonthValue());
        Path targetDir = uploadDir.resolve(subDir);
        Files.createDirectories(targetDir);

        // Generate unique filename preserving extension
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;

        // Save file to disk
        Path targetPath = targetDir.resolve(storedName);
        file.transferTo(targetPath.toFile());

        // Create database record
        String relativePath = "/uploads/" + subDir + "/" + storedName;
        CmsFile cmsFile = new CmsFile();
        cmsFile.setOriginalName(originalName != null ? originalName : storedName);
        cmsFile.setStoredName(storedName);
        cmsFile.setFilePath(relativePath);
        cmsFile.setMimeType(contentType);
        cmsFile.setFileSize(file.getSize());
        cmsFile.setCreatedAt(Instant.now());
        cmsFile.setCreatedBy(getCurrentUser());

        return cmsFileRepository.save(cmsFile);
    }

    public void delete(Long fileId) throws IOException {
        CmsFile cmsFile = cmsFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // Remove the leading "/uploads/" to get relative path from upload dir
        String relativePath = cmsFile.getFilePath().replaceFirst("^/uploads/", "");
        Path filePath = uploadDir.resolve(relativePath);
        Files.deleteIfExists(filePath);

        cmsFileRepository.delete(cmsFile);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return null;
    }
}
