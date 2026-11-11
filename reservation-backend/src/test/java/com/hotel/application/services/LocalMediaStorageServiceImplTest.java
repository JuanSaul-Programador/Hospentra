package com.hotel.application.services;

import com.hotel.application.services.impl.LocalMediaStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalMediaStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalMediaStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalMediaStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "baseUploadDir", tempDir.toString());
    }

    @Test
    void uploadFile_ValidJpegMagicBytes_SavesWithJpgExtension() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "suite.jpeg", "image/jpeg", jpegBytes);

        String url = storageService.uploadFile(file, "rooms");

        assertThat(url).startsWith("/uploads/rooms/").endsWith(".jpg");
        String fileName = url.substring("/uploads/rooms/".length());
        assertThat(Files.exists(tempDir.resolve("rooms").resolve(fileName))).isTrue();
    }

    @Test
    void uploadFile_ValidPngMagicBytes_SavesWithPngExtension() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "suite.png", "image/png", pngBytes);

        String url = storageService.uploadFile(file, "rooms");

        assertThat(url).startsWith("/uploads/rooms/").endsWith(".png");
    }

    @Test
    void uploadFile_ValidWebpMagicBytes_SavesWithWebpExtension() {
        byte[] webpBytes = new byte[]{'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 0x56};
        MockMultipartFile file = new MockMultipartFile("file", "suite.webp", "image/webp", webpBytes);

        String url = storageService.uploadFile(file, "rooms");

        assertThat(url).startsWith("/uploads/rooms/").endsWith(".webp");
    }

    @Test
    void uploadFile_MaliciousHtmlExtensionWithRealJpegBytes_IgnoresOriginalExtension() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "exploit.html", "image/jpeg", jpegBytes);

        String url = storageService.uploadFile(file, "rooms");

        assertThat(url).endsWith(".jpg");
        assertThat(url).doesNotContain(".html");
    }

    @Test
    void uploadFile_SvgPayloadSpoofingImageJpegContentType_ThrowsIllegalArgumentException() {
        byte[] svgBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", svgBytes);

        assertThatThrownBy(() -> storageService.uploadFile(file, "rooms"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imágenes reales");
    }

    @Test
    void uploadFile_HtmlPayload_ThrowsIllegalArgumentException() {
        byte[] htmlBytes = "<html><script>alert('xss')</script></html>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "xss.html", "text/html", htmlBytes);

        assertThatThrownBy(() -> storageService.uploadFile(file, "rooms"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadFile_SvgContentType_ThrowsIllegalArgumentException() {
        byte[] svgBytes = "<svg></svg>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "icon.svg", "image/svg+xml", svgBytes);

        assertThatThrownBy(() -> storageService.uploadFile(file, "rooms"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadFile_Exceeding5Mb_ThrowsIllegalArgumentException() {
        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
        oversized[0] = (byte) 0xFF;
        oversized[1] = (byte) 0xD8;
        oversized[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> storageService.uploadFile(file, "rooms"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void uploadFile_PathTraversalInSubDirectory_ThrowsSecurityException() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile("file", "suite.jpg", "image/jpeg", jpegBytes);

        assertThatThrownBy(() -> storageService.uploadFile(file, "../evil"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> storageService.uploadFile(file, "rooms/../../etc"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteFile_PathTraversalOutsideBaseDir_DoesNotDeleteExternalFile() throws Exception {
        Path externalFile = Files.createTempFile("hospentra-external-", ".txt");
        try {
            String traversalUrl = "/uploads/../" + externalFile.getFileName();
            storageService.deleteFile(traversalUrl);
            assertThat(Files.exists(externalFile)).isTrue();
        } finally {
            Files.deleteIfExists(externalFile);
        }
    }
}
