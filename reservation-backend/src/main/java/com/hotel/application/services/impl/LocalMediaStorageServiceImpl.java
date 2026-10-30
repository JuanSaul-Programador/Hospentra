package com.hotel.application.services.impl;

import com.hotel.application.services.MediaStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LocalMediaStorageServiceImpl implements MediaStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Value("${app.storage.local-dir:uploads}")
    private String baseUploadDir;

    @Override
    public String uploadFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido de 10 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato no compatible. Solo se admiten imágenes JPG, PNG, WEBP o AVIF.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String safeFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path targetFolder = Paths.get(baseUploadDir, subDirectory).toAbsolutePath().normalize();
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            Path targetPath = targetFolder.resolve(safeFileName).normalize();
            if (!targetPath.startsWith(targetFolder)) {
                throw new SecurityException("Ruta de archivo no permitida.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Media file successfully saved locally: {}", targetPath);
            return "/uploads/" + subDirectory + "/" + safeFileName;
        } catch (IOException e) {
            log.error("Failed to persist media file locally", e);
            throw new RuntimeException("Error al guardar la imagen en el almacenamiento local.", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            String relativePath = fileUrl.substring("/uploads/".length());
            Path targetFile = Paths.get(baseUploadDir).resolve(relativePath).toAbsolutePath().normalize();
            Path baseFolder = Paths.get(baseUploadDir).toAbsolutePath().normalize();

            if (targetFile.startsWith(baseFolder) && Files.exists(targetFile)) {
                Files.delete(targetFile);
                log.info("Media file successfully removed from disk: {}", targetFile);
            }
        } catch (Exception e) {
            log.warn("Could not delete physical media file: {}", fileUrl, e);
        }
    }
}
