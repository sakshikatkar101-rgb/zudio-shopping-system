package com.zudio.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zudio.config.AppConfig;
import com.zudio.util.JsonUtil;
import com.zudio.util.TokenUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set Enterprise Security & CORS Headers
        String allowedOrigin = AppConfig.getAllowedOrigin();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin != null ? allowedOrigin : "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");

        // Hardened HTTP Security Headers
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "SAMEORIGIN");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https://images.unsplash.com; " +
                "connect-src 'self';");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            process(exchange);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            sendError(exchange, 403, e.getMessage());
        } catch (Exception e) {
            // Log full exception details ONLY on server console for debugging
            System.err.println("[SERVER ERROR at " + exchange.getRequestURI() + "]: " + e.getMessage());
            e.printStackTrace();
            // Return safe, sanitized message to external clients (NO stack traces / NO SQL leaks)
            sendError(exchange, 500, "An internal server error occurred. Please try again later.");
        }
    }

    protected abstract void process(HttpExchange exchange) throws Exception;

    protected void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        if (message != null) map.put("message", message);
        if (data != null) map.put("data", data);
        sendJson(exchange, 200, map);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("error", message);
        sendJson(exchange, statusCode, map);
    }

    protected String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected String getClientIp(HttpExchange exchange) {
        String xForwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRemoteAddress();
        return remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "127.0.0.1";
    }

    protected Map<String, String> getQueryParams(HttpExchange exchange) {
        Map<String, String> queryParams = new HashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) return queryParams;

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    queryParams.put(key, val);
                } else if (idx < 0) {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    queryParams.put(key, "");
                }
            } catch (Exception ignored) {}
        }
        return queryParams;
    }

    protected Integer extractPathId(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith(prefix)) {
            String remainder = path.substring(prefix.length());
            if (remainder.startsWith("/")) remainder = remainder.substring(1);
            int slash = remainder.indexOf("/");
            String idStr = slash >= 0 ? remainder.substring(0, slash) : remainder;
            try {
                return Integer.parseInt(idStr);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    protected TokenUtil.SessionUser getSessionUser(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = TokenUtil.extractToken(authHeader);
        if (token != null) {
            return TokenUtil.validateToken(token);
        }
        return null;
    }

    protected TokenUtil.SessionUser requireAuth(HttpExchange exchange) {
        TokenUtil.SessionUser user = getSessionUser(exchange);
        if (user == null) {
            throw new SecurityException("Authentication required. Please log in.");
        }
        return user;
    }

    protected TokenUtil.SessionUser requireAdmin(HttpExchange exchange) {
        TokenUtil.SessionUser user = requireAuth(exchange);
        if (!user.isAdmin()) {
            throw new SecurityException("Administrator access required for this action.");
        }
        return user;
    }
}
