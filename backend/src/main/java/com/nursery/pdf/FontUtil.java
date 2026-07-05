package com.nursery.pdf;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Base64;

public final class FontUtil {

    private FontUtil() {}

    public static String loadFontAsBase64(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);

            byte[] bytes = resource.getInputStream().readAllBytes();

            return Base64.getEncoder().encodeToString(bytes);

        } catch (IOException e) {
            throw new RuntimeException("Unable to load font: " + path, e);
        }
    }
}