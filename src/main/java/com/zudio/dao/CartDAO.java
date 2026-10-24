package com.zudio.dao;

import com.zudio.model.CartItem;
import com.zudio.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    public List<CartItem> getCartByUser(int userId) {
        String sql = "SELECT ci.id, ci.user_id, ci.product_id, ci.size, ci.quantity, " +
                "p.name AS product_name, p.image_url, p.price, p.mrp, p.stock " +
                "FROM cart_items ci " +
                "JOIN products p ON ci.product_id = p.id " +
                "WHERE ci.user_id = ? ORDER BY ci.created_at DESC";
        List<CartItem> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CartItem item = new CartItem();
                        item.setId(rs.getInt("id"));
                        item.setUserId(rs.getInt("user_id"));
                        item.setProductId(rs.getInt("product_id"));
                        item.setSize(rs.getString("size"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setProductName(rs.getString("product_name"));
                        item.setProductImageUrl(rs.getString("image_url"));
                        item.setProductPrice(rs.getBigDecimal("price"));
                        item.setProductMrp(rs.getBigDecimal("mrp"));
                        item.setStockAvailable(rs.getInt("stock"));
                        list.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO getCartByUser]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public boolean addItem(int userId, int productId, String size, int quantity) {
        if (size == null || size.isBlank()) size = "M";
        if (quantity <= 0) quantity = 1;

        String checkSql = "SELECT id, quantity FROM cart_items WHERE user_id = ? AND product_id = ? AND size = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE id = ?";
        String insertSql = "INSERT INTO cart_items (user_id, product_id, size, quantity) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            int existingId = -1;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, productId);
                ps.setString(3, size);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, existingId);
                    return ps.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setString(3, size);
                    ps.setInt(4, quantity);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO addItem]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean updateQuantity(int cartItemId, int userId, int quantity) {
        if (quantity <= 0) {
            return removeItem(cartItemId, userId);
        }
        String sql = "UPDATE cart_items SET quantity = ? WHERE id = ? AND user_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, quantity);
                ps.setInt(2, cartItemId);
                ps.setInt(3, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO updateQuantity]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean removeItem(int cartItemId, int userId) {
        String sql = "DELETE FROM cart_items WHERE id = ? AND user_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, cartItemId);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO removeItem]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean clearCart(int userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                return ps.executeUpdate() >= 0;
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO clearCart]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }
}
