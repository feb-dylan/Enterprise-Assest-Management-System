package com.eams.service;

import com.eams.config.AppProperties;
import com.eams.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Safe image storage.
 *
 * - Only JPEG / PNG / WEBP are accepted.
 * - The type is detected from the file's first bytes ("magic number"),
 *   NOT from the client's Content-Type or file name, which can be faked.
 * - The server chooses the file name and extension, so an upload can
 *   never become an .html or .js file served from /uploads.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    public record StoredFile(String url, Path path, String contentType) {}

    private final AppProperties properties;

    public StoredFile storeImage(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        ImageType type = detect(file);
        if (type == null) {
            throw new BadRequestException("Only JPEG, PNG or WEBP images are allowed");
        }

        String folder = subFolder.replaceAll("[^a-zA-Z0-9_-]", "");
        Path dir = Paths.get(properties.uploadDir(), folder).toAbsolutePath().normalize();
        String fileName = UUID.randomUUID() + type.extension;
        Path target = dir.resolve(fileName).normalize();

        if (!target.startsWith(dir)) {
            throw new BadRequestException("Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dir);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }

        return new StoredFile("/uploads/" + folder + "/" + fileName, target, type.contentType);
    }

    /** Resolves a public /uploads/... URL back to a file on disk (for AI analysis). */
    public Path resolveUrl(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return null;
        }
        Path root = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        Path path = root.resolve(url.substring("/uploads/".length())).normalize();
        return path.startsWith(root) ? path : null;
    }

    // ---------------------------------------------------------

    private enum ImageType {
        JPEG(".jpg", "image/jpeg"),
        PNG(".png", "image/png"),
        WEBP(".webp", "image/webp");

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }
    }

    private ImageType detect(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return null;
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8
                && (head[2] & 0xFF) == 0xFF) {
            return ImageType.JPEG;
        }
        if (read >= 8 && Arrays.equals(Arrays.copyOf(head, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return ImageType.PNG;
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return ImageType.WEBP;
        }
        return null;
    }
}
