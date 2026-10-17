package com.nursery.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class PdfRenderer {

    private static final int MAX_CONCURRENT_RENDERS = 2;
    private static final long ACQUIRE_TIMEOUT_SECONDS = 10;
    private static final long RENDER_TIMEOUT_SECONDS = 30;

    @Value("${app.pdf.weasyprint-path:weasyprint}")
    private String executable;

    private final Semaphore slots = new Semaphore(MAX_CONCURRENT_RENDERS);

    public byte[] generatePdf(String html) {
        try {
            if (!slots.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("PDF renderer is busy, please try again");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for PDF renderer", e);
        }

        Process process = null;
        try {
            try {
                ProcessBuilder builder = new ProcessBuilder(resolveExecutable(), "--presentational-hints", "-", "-");
                addWindowsNativeLibraryPath(builder);
                process = builder.start();
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Unable to start WeasyPrint at '" + executable
                                + "'. Install WeasyPrint and its native GTK/Pango runtime, "
                                + "or set WEASYPRINT_PATH to the executable path.",
                        e);
            }
            final Process proc = process;

            Thread writer = new Thread(() -> {
                try (OutputStream os = proc.getOutputStream()) {
                    os.write(html.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // The exit code check below reports child-process failures.
                }
            }, "pdf-stdin-writer");
            writer.start();

            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return "";
                }
            });

            byte[] pdf = process.getInputStream().readAllBytes();

            if (!process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("weasyprint timed out after " + RENDER_TIMEOUT_SECONDS + "s");
            }
            writer.join();

            if (process.exitValue() != 0 || pdf.length < 5) {
                throw new IOException("weasyprint failed (exit " + process.exitValue() + "): "
                        + stderr.get(2, TimeUnit.SECONDS));
            }
            return pdf;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            slots.release();
        }
    }

    private String resolveExecutable() {
        if (!"weasyprint".equals(executable) || !isWindows()) {
            return executable;
        }

        String appData = System.getenv("APPDATA");
        if (appData == null) {
            return executable;
        }

        Path pythonDirectory = Path.of(appData, "Python");
        if (!Files.isDirectory(pythonDirectory)) {
            return executable;
        }

        try (Stream<Path> versions = Files.list(pythonDirectory)) {
            return versions
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("Python"))
                    .map(path -> path.resolve("Scripts").resolve("weasyprint.exe"))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .map(Path::toString)
                    .orElse(executable);
        } catch (IOException e) {
            return executable;
        }
    }

    private void addWindowsNativeLibraryPath(ProcessBuilder builder) {
        if (!isWindows()) {
            return;
        }

        Path gtkBin = Path.of("C:", "msys64", "mingw64", "bin");
        if (Files.isDirectory(gtkBin)) {
            String path = builder.environment().getOrDefault("PATH", "");
            builder.environment().put("PATH", gtkBin + ";" + path);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}