package com.zudio.handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.zudio.model.User;
import com.zudio.service.AuthService;
import com.zudio.util.JsonUtil;
import com.zudio.util.LoginRateLimiter;
import com.zudio.util.TokenUtil;

import java.util.Map;

public class AuthHandler extends BaseHttpHandler {
    private final AuthService authService = new AuthService();

    @Override
    protected void process(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith("/register") && "POST".equals(method)) {
            handleRegister(exchange);
        } else if (path.endsWith("/login") && "POST".equals(method)) {
            handleLogin(exchange);
        } else if (path.endsWith("/me") && "GET".equals(method)) {
            handleMe(exchange);
        } else if (path.endsWith("/logout") && "POST".equals(method)) {
            handleLogout(exchange);
        } else {
            sendError(exchange, 404, "Endpoint not found: " + path);
        }
    }

    private void handleRegister(HttpExchange exchange) throws Exception {
        String body = readBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);

        String name = json.has("name") ? json.get("name").getAsString() : "";
        String email = json.has("email") ? json.get("email").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";
        String phone = json.has("phone") ? json.get("phone").getAsString() : null;

        Map<String, Object> result = authService.register(name, email, password, phone);
        sendSuccess(exchange, "Account registered successfully!", result);
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        String body = readBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);

        String email = json.has("email") ? json.get("email").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";
        String clientIp = getClientIp(exchange);

        // 1. Rate Limiting Check (Max 5 attempts per 10 minutes)
        if (LoginRateLimiter.isBlocked(clientIp, email)) {
            sendError(exchange, 429, "Too many failed login attempts. Please wait 10 minutes before trying again.");
            return;
        }

        try {
            Map<String, Object> result = authService.login(email, password);
            // On success, reset failed attempt count
            LoginRateLimiter.recordSuccess(clientIp, email);
            sendSuccess(exchange, "Login successful!", result);
        } catch (IllegalArgumentException e) {
            // Record failure for IP and email
            LoginRateLimiter.recordFailedAttempt(clientIp, email);
            sendError(exchange, 401, e.getMessage());
        }
    }

    private void handleMe(HttpExchange exchange) throws Exception {
        TokenUtil.SessionUser session = requireAuth(exchange);
        User profile = authService.getProfile(session.getUserId());
        if (profile == null) {
            sendError(exchange, 404, "User profile not found.");
            return;
        }
        sendSuccess(exchange, null, profile);
    }

    private void handleLogout(HttpExchange exchange) throws Exception {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = TokenUtil.extractToken(authHeader);
        if (token != null) {
            authService.logout(token);
        }
        sendSuccess(exchange, "Logged out successfully.", null);
    }
}
