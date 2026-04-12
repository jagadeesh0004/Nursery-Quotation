package com.nursery.profile.service;

import com.nursery.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.logos-dir}")
    private String logosDir;

    @Value("${app.file.pdfs-dir}")
    private String pdfsDir;

    @PostConstruct
    public void init() {
        createDirectorySafely(logosDir);
        createDirectorySafely(pdfsDir);
    }

    public String storePdf(byte[] pdfBytes, String filename) {
        Path target = Paths.get(pdfsDir).resolve(filename);
        try {
            Files.write(target, pdfBytes);
            log.info("PDF saved: {}", target);
            return target.toString();
        } catch (IOException e) {
            throw new BusinessException("Failed to store PDF: " + e.getMessage());
        }
    }

    public byte[] readFile(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new BusinessException("Failed to read file: " + filePath);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath);
        }
    }

    public boolean fileExists(String filePath) {
        return filePath != null && Files.exists(Paths.get(filePath));
    }

    private void createDirectorySafely(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            log.error("Failed to create directory: {}", dir, e);
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
