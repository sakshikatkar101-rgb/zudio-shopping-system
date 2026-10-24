package com.zudio.dao;

import com.zudio.model.Coupon;
import com.zudio.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    public Coupon findByCode(String code) {
        if (code == null || code.isBlank()) return null;
        String sql = "SELECT code, discount_percent, active, min_order_amount, created_at FROM coupons WHERE UPPER(code) = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code.trim().toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Coupon(
                                rs.getString("code"),
                                rs.getInt("discount_percent"),
                                rs.getBoolean("active"),
                                rs.getBigDecimal("min_order_amount"),
                                rs.getTimestamp("created_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CouponDAO findByCode]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }

    public List<Coupon> listAll() {
        String sql = "SELECT code, discount_percent, active, min_order_amount, created_at FROM coupons ORDER BY discount_percent DESC";
        List<Coupon> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Coupon(
                            rs.getString("code"),
                            rs.getInt("discount_percent"),
                            rs.getBoolean("active"),
                            rs.getBigDecimal("min_order_amount"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CouponDAO listAll]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }
}
