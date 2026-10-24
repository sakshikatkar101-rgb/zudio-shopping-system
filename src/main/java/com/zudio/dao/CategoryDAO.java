package com.zudio.dao;

import com.zudio.model.Category;
import com.zudio.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> listAll() {
        String sql = "SELECT id, name, description, image_url FROM categories ORDER BY id ASC";
        List<Category> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO listAll]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public Category findById(int id) {
        String sql = "SELECT id, name, description, image_url FROM categories WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Category(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getString("image_url")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO findById]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }
}
