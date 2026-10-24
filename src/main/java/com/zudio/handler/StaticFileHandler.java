package com.zudio.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zudio.config.AppConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Hardened Static File Handler.
 * Features:
 * - Anti-path traversal canonical path validation
 * - Forbidden file/source blocklist (.sql, .properties, .java, .class, .xml, .env, etc.)
 * - GZIP response compression
 * - Cache-Control headers
 * - Safe fallback to index.html for Single-Page Application routes
 */
public class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "properties", "sql", "java", "class", "xml", "yml", "yaml", "env",
            "bat", "sh", "ps1", "log", "git", "md", "jsonl", "bak"
    );

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String rawReqPath = exchange.getRequestURI().getPath();
        if (rawReqPath == null || rawReqPath.equals("/") || rawReqPath.isBlank()) {
            rawReqPath = "/index.html";
        }

        // 1. URL Decode safely
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(rawReqPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            sendResponse(exchange, 400, "text/plain", "Bad Request".getBytes());
            return;
        }

        // 2. Strict Forbidden File Check
        String ext = getExtension(decodedPath).toLowerCase();
        if (FORBIDDEN_EXTENSIONS.contains(ext) || decodedPath.toLowerCase().contains("web-inf") || decodedPath.toLowerCase().contains(".git")) {
            sendResponse(exchange, 403, "text/plain", "403 Forbidden: Access denied to protected file type.".getBytes());
            return;
        }

        // 3. Resolve base directory and canonical path
        Path baseDir = Paths.get(AppConfig.getFrontendDir()).toAbsolutePath().normalize();
        String relativeSub = decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;
        Path targetPath = baseDir.resolve(relativeSub).normalize();

        // 4. Anti-Path Traversal Check
        if (!targetPath.startsWith(baseDir)) {
            System.err.println("[Security Alert] Path traversal attempt blocked: " + rawReqPath);
            sendResponse(exchange, 403, "text/plain", "403 Forbidden: Path traversal attempt blocked.".getBytes());
            return;
        }

        // 5. Load file content: File system first, Classpath second (fat JAR), SPA fallback third
        byte[] content = null;
        String actualFilename = null;
        boolean isSpaFallback = false;

        // Try filesystem first
        if (Files.exists(targetPath) && !Files.isDirectory(targetPath)) {
            content = Files.readAllBytes(targetPath);
            actualFilename = targetPath.getFileName().toString();
        }

        // If not found on filesystem, try classpath (fat JAR resource)
        if (content == null) {
            String cpResourcePath = "/frontend/" + relativeSub.replace('\\', '/');
            try (InputStream in = getClass().getResourceAsStream(cpResourcePath)) {
                if (in != null) {
                    content = in.readAllBytes();
                    actualFilename = relativeSub;
                }
            }
        }

        // If still not found, check if it's an SPA route / page fallback
        if (content == null) {
            boolean hasAssetExt = ext.matches("^(js|css|png|jpg|jpeg|svg|gif|ico|woff|woff2|ttf|json|map)$");
            if (!hasAssetExt) {
                // Try index.html from filesystem
                Path indexPath = baseDir.resolve("index.html").normalize();
                if (Files.exists(indexPath) && !Files.isDirectory(indexPath)) {
                    content = Files.readAllBytes(indexPath);
                    actualFilename = "index.html";
                    isSpaFallback = true;
                } else {
                    // Try index.html from classpath
                    try (InputStream in = getClass().getResourceAsStream("/frontend/index.html")) {
                        if (in != null) {
                            content = in.readAllBytes();
                            actualFilename = "index.html";
                            isSpaFallback = true;
                        }
                    }
                }
            }
        }

        if (content == null) {
            sendResponse(exchange, 404, "text/plain", "404 Not Found".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String actualExt = getExtension(actualFilename != null ? actualFilename : decodedPath);
        String contentType = MIME_TYPES.getOrDefault(actualExt.toLowerCase(), "application/octet-stream");

        // 6. Security Headers
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "SAMEORIGIN");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");

        // 7. Caching Policy
        if (actualExt.equalsIgnoreCase("html") || isSpaFallback) {
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        } else {
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400"); // 1 day
        }

        // 8. GZIP Compression (if client supports and content > 512 bytes)
        String acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip") && content.length > 512) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(content);
            }
            byte[] compressed = baos.toByteArray();
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            sendResponse(exchange, 200, contentType, compressed);
        } else {
            sendResponse(exchange, 200, contentType, content);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1);
        }
        return "";
    }
}
