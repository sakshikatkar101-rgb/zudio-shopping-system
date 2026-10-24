package com.zudio.dao;

import com.zudio.model.WishlistItem;
import com.zudio.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAO {

    public List<WishlistItem> getWishlistByUser(int userId) {
        String sql = "SELECT w.id, w.user_id, w.product_id, w.created_at, " +
                "p.name AS product_name, p.image_url, p.price, p.mrp, p.stock " +
                "FROM wishlist w " +
                "JOIN products p ON w.product_id = p.id " +
                "WHERE w.user_id = ? ORDER BY w.created_at DESC";
        List<WishlistItem> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WishlistItem item = new WishlistItem();
                        item.setId(rs.getInt("id"));
                        item.setUserId(rs.getInt("user_id"));
                        item.setProductId(rs.getInt("product_id"));
                        item.setProductName(rs.getString("product_name"));
                        item.setProductImageUrl(rs.getString("image_url"));
                        item.setProductPrice(rs.getBigDecimal("price"));
                        item.setProductMrp(rs.getBigDecimal("mrp"));
                        item.setProductStock(rs.getInt("stock"));
                        item.setCreatedAt(rs.getTimestamp("created_at"));
                        list.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO getWishlistByUser]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public boolean toggleItem(int userId, int productId) {
        String checkSql = "SELECT id FROM wishlist WHERE user_id = ? AND product_id = ?";
        String deleteSql = "DELETE FROM wishlist WHERE user_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO wishlist (user_id, product_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) exists = true;
                }
            }

            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                    return false; // removed
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                    return true; // added
                }
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO toggleItem]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean isWishlisted(int userId, int productId) {
        String sql = "SELECT id FROM wishlist WHERE user_id = ? AND product_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO isWishlisted]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean removeItem(int userId, int productId) {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND product_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, productId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO removeItem]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }
}
