package com.hotel.application.services.impl;

import com.hotel.application.services.MediaStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalMediaStorageServiceImpl implements MediaStorageService {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Value("${app.storage.local-dir:uploads}")
    private String baseUploadDir;

    @Override
    public String uploadFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido de 5 MB.");
        }

        if (subDirectory == null || subDirectory.isBlank() || !subDirectory.matches("^[a-zA-Z0-9_-]+$")) {
            throw new SecurityException("Ruta de archivo no permitida.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.trim().toLowerCase())) {
            throw new IllegalArgumentException("Formato no compatible. Solo se admiten imágenes JPEG, PNG o WEBP.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo de imagen.", e);
        }

        if (content.length == 0) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío.");
        }

        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido de 5 MB.");
        }

        String safeExtension = detectSafeExtensionFromMagicBytes(content);
        if (safeExtension == null) {
            throw new IllegalArgumentException("Formato de archivo no válido. Solo se permiten imágenes reales JPEG, PNG o WEBP.");
        }

        String safeFileName = UUID.randomUUID().toString().replace("-", "") + safeExtension;

        try {
            Path baseFolder = Paths.get(baseUploadDir).toAbsolutePath().normalize();
            Path targetFolder = baseFolder.resolve(subDirectory).normalize();
            if (!targetFolder.startsWith(baseFolder)) {
                throw new SecurityException("Ruta de archivo no permitida.");
            }

            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            Path targetPath = targetFolder.resolve(safeFileName).normalize();
            if (!targetPath.startsWith(targetFolder) || !targetPath.startsWith(baseFolder)) {
                throw new SecurityException("Ruta de archivo no permitida.");
            }

            Files.write(targetPath, content);

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
            Path baseFolder = Paths.get(baseUploadDir).toAbsolutePath().normalize();
            Path targetFile = baseFolder.resolve(relativePath).normalize();

            if (targetFile.startsWith(baseFolder) && Files.isRegularFile(targetFile)) {
                Files.delete(targetFile);
                log.info("Media file successfully removed from disk: {}", targetFile);
            }
        } catch (Exception e) {
            log.warn("Could not delete physical media file: {}", fileUrl, e);
        }
    }

    private String detectSafeExtensionFromMagicBytes(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return ".png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
