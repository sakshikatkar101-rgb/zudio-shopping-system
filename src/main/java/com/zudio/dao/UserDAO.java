package com.zudio.dao;

import com.zudio.model.User;
import com.zudio.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT id, name, email, password_hash, role, phone, created_at FROM users WHERE email = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO findByEmail]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT id, name, email, password_hash, role, phone, created_at FROM users WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO findById]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }

    public User create(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role, phone) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail().trim().toLowerCase());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getRole() != null ? user.getRole() : "CUSTOMER");
                ps.setString(5, user.getPhone());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                    }
                }
            }
            return user;
        } catch (SQLException e) {
            System.err.println("[UserDAO create]: " + e.getMessage());
            throw new RuntimeException("Could not create user: " + e.getMessage(), e);
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public List<User> listAllCustomers() {
        String sql = "SELECT id, name, email, password_hash, role, phone, created_at FROM users WHERE role = 'CUSTOMER' ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO listAllCustomers]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public int countCustomers() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO countCustomers]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return 0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setPhone(rs.getString("phone"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
