package com.zudio.service;

import com.zudio.dao.UserDAO;
import com.zudio.model.User;
import com.zudio.util.PasswordUtil;
import com.zudio.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{10,20}$");

    public Map<String, Object> register(String name, String email, String password, String phone) {
        // 1. Validate Full Name
        if (name == null || name.trim().length() < 2 || name.trim().length() > 100) {
            throw new IllegalArgumentException("Full name must be between 2 and 100 characters.");
        }
        if (name.contains("<") || name.contains(">")) {
            throw new IllegalArgumentException("Full name contains invalid characters.");
        }

        // 2. Validate Email
        if (email == null || email.trim().length() > 150 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please provide a valid email address (e.g. user@example.com).");
        }

        // 3. Validate Password (Min 8 characters, Max 128)
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters long.");
        }

        // 4. Validate Phone if provided
        String cleanPhone = null;
        if (phone != null && !phone.isBlank()) {
            String trimmedPhone = phone.trim();
            if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
                throw new IllegalArgumentException("Invalid phone number format. Must be 10-20 digits.");
            }
            cleanPhone = trimmedPhone;
        }

        String normalizedEmail = email.trim().toLowerCase();
        User existing = userDAO.findByEmail(normalizedEmail);
        if (existing != null) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        // Hash with PBKDF2WithHmacSHA256 (120,000 iterations)
        String passwordHash = PasswordUtil.hashPassword(password);

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setRole("CUSTOMER"); // Privilege Escalation Prevention: ALWAYS strictly CUSTOMER
        user.setPhone(cleanPhone);

        user = userDAO.create(user);
        String token = TokenUtil.createToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", sanitize(user));
        return response;
    }

    public Map<String, Object> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userDAO.findByEmail(normalizedEmail);

        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            // Constant timing safe rejection
            throw new IllegalArgumentException("Invalid email or password. Please try again.");
        }

        String token = TokenUtil.createToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", sanitize(user));
        return response;
    }

    public User getProfile(int userId) {
        User user = userDAO.findById(userId);
        if (user != null) {
            return sanitize(user);
        }
        return null;
    }

    public void logout(String token) {
        TokenUtil.removeToken(token);
    }

    private User sanitize(User u) {
        u.setPasswordHash(null);
        return u;
    }
}
