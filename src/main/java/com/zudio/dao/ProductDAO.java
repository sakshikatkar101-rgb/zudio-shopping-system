package com.zudio.dao;

import com.zudio.model.Product;
import com.zudio.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> listProducts(String categoryName, String search, BigDecimal minPrice, 
                                     BigDecimal maxPrice, String sortBy, Boolean trendingOnly) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.name, p.description, p.price, p.mrp, p.category_id, " +
                "c.name AS category_name, p.image_url, p.stock, p.rating, p.is_trending, p.created_at " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (categoryName != null && !categoryName.isBlank() && !categoryName.equalsIgnoreCase("all")) {
            sql.append("AND LOWER(c.name) = ? ");
            params.add(categoryName.trim().toLowerCase());
        }

        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        if (minPrice != null) {
            sql.append("AND p.price >= ? ");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            sql.append("AND p.price <= ? ");
            params.add(maxPrice);
        }

        if (trendingOnly != null && trendingOnly) {
            sql.append("AND p.is_trending = TRUE ");
        }

        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "price_asc":
                    sql.append("ORDER BY p.price ASC ");
                    break;
                case "price_desc":
                    sql.append("ORDER BY p.price DESC ");
                    break;
                case "rating":
                    sql.append("ORDER BY p.rating DESC ");
                    break;
                case "newest":
                    sql.append("ORDER BY p.created_at DESC ");
                    break;
                case "popularity":
                default:
                    sql.append("ORDER BY p.is_trending DESC, p.rating DESC, p.id DESC ");
                    break;
            }
        } else {
            sql.append("ORDER BY p.is_trending DESC, p.rating DESC, p.id DESC ");
        }

        List<Product> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO listProducts]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public Product findById(int id) {
        String sql = "SELECT p.id, p.name, p.description, p.price, p.mrp, p.category_id, " +
                "c.name AS category_name, p.image_url, p.stock, p.rating, p.is_trending, p.created_at " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id WHERE p.id = ?";
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
            System.err.println("[ProductDAO findById]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }

    public Product create(Product p) {
        String sql = "INSERT INTO products (name, description, price, mrp, category_id, image_url, stock, rating, is_trending) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription());
                ps.setBigDecimal(3, p.getPrice());
                ps.setBigDecimal(4, p.getMrp() != null ? p.getMrp() : p.getPrice());
                if (p.getCategoryId() != null) {
                    ps.setInt(5, p.getCategoryId());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                ps.setString(6, p.getImageUrl());
                ps.setInt(7, p.getStock());
                ps.setBigDecimal(8, p.getRating() != null ? p.getRating() : new BigDecimal("4.5"));
                ps.setBoolean(9, p.isTrending());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
            }
            return p;
        } catch (SQLException e) {
            System.err.println("[ProductDAO create]: " + e.getMessage());
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, mrp = ?, category_id = ?, " +
                "image_url = ?, stock = ?, rating = ?, is_trending = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription());
                ps.setBigDecimal(3, p.getPrice());
                ps.setBigDecimal(4, p.getMrp());
                if (p.getCategoryId() != null) {
                    ps.setInt(5, p.getCategoryId());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                ps.setString(6, p.getImageUrl());
                ps.setInt(7, p.getStock());
                ps.setBigDecimal(8, p.getRating());
                ps.setBoolean(9, p.isTrending());
                ps.setInt(10, p.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO update]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO delete]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public boolean updateStock(int id, int stock) {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, stock);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO updateStock]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    public List<Product> getLowStockAlerts(int threshold) {
        String sql = "SELECT p.id, p.name, p.description, p.price, p.mrp, p.category_id, " +
                "c.name AS category_name, p.image_url, p.stock, p.rating, p.is_trending, p.created_at " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.stock <= ? ORDER BY p.stock ASC LIMIT 10";
        List<Product> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, threshold);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO getLowStockAlerts]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public int countTotalProducts() {
        String sql = "SELECT COUNT(*) FROM products";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO countTotalProducts]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return 0;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setMrp(rs.getBigDecimal("mrp"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setImageUrl(rs.getString("image_url"));
        p.setStock(rs.getInt("stock"));
        p.setRating(rs.getBigDecimal("rating"));
        p.setTrending(rs.getBoolean("is_trending"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
