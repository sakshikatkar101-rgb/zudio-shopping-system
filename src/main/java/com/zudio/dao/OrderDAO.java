package com.zudio.dao;

import com.zudio.model.CartItem;
import com.zudio.model.Order;
import com.zudio.model.OrderItem;
import com.zudio.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderDAO {

    /**
     * Executes order placement inside a pure ACID database transaction:
     * 1. Inserts into orders table
     * 2. For each item: locks product row with FOR UPDATE, verifies stock, decrements stock, inserts order_items
     * 3. Clears user cart
     * 4. Commits on success, or rollbacks on any failure.
     */
    public Order placeOrder(int userId, BigDecimal totalAmount, BigDecimal discount, 
                           String paymentMethod, String address, List<CartItem> items) throws Exception {
        return placeOrder(userId, totalAmount, discount, paymentMethod, address, items, null, null, null);
    }

    public Order placeOrder(int userId, BigDecimal totalAmount, BigDecimal discount, 
                           String paymentMethod, String address, List<CartItem> items,
                           String paymentStatus, String cardLast4, String cardBrand) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart");
        }

        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            // Default payment status: PAID for online simulated payments, PENDING for COD
            String initialPaymentStatus = paymentStatus;
            if (initialPaymentStatus == null || initialPaymentStatus.isBlank()) {
                if ("CARD".equalsIgnoreCase(paymentMethod) || "UPI".equalsIgnoreCase(paymentMethod)) {
                    initialPaymentStatus = "PAID";
                } else {
                    initialPaymentStatus = "PENDING";
                }
            }

            // 1. Insert Order
            String orderSql = "INSERT INTO orders (user_id, total_amount, discount, status, payment_method, payment_status, card_last4, card_brand, address) " +
                    "VALUES (?, ?, ?, 'Placed', ?, ?, ?, ?, ?)";
            int orderId = -1;
            try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setBigDecimal(2, totalAmount);
                ps.setBigDecimal(3, discount != null ? discount : BigDecimal.ZERO);
                ps.setString(4, paymentMethod);
                ps.setString(5, initialPaymentStatus);
                ps.setString(6, cardLast4);
                ps.setString(7, cardBrand);
                ps.setString(8, address);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }

            if (orderId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to generate order identifier");
            }

            // 2. Process Order Items & Check/Deduct Stock
            String lockStockSql = "SELECT stock, price, name FROM products WHERE id = ? FOR UPDATE";
            String deductStockSql = "UPDATE products SET stock = stock - ? WHERE id = ?";
            String insertItemSql = "INSERT INTO order_items (order_id, product_id, size, quantity, price) VALUES (?, ?, ?, ?, ?)";

            List<OrderItem> createdItems = new ArrayList<>();

            for (CartItem item : items) {
                // Verify & Lock Stock
                try (PreparedStatement psLock = conn.prepareStatement(lockStockSql)) {
                    psLock.setInt(1, item.getProductId());
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalStateException("Product not found: ID " + item.getProductId());
                        }
                        int currentStock = rs.getInt("stock");
                        String pName = rs.getString("name");
                        if (currentStock < item.getQuantity()) {
                            conn.rollback();
                            throw new IllegalStateException("Insufficient stock for '" + pName + "'. Available: " + currentStock + ", Requested: " + item.getQuantity());
                        }
                    }
                }

                // Deduct Stock
                try (PreparedStatement psDeduct = conn.prepareStatement(deductStockSql)) {
                    psDeduct.setInt(1, item.getQuantity());
                    psDeduct.setInt(2, item.getProductId());
                    psDeduct.executeUpdate();
                }

                // Insert Order Item
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                    psItem.setInt(1, orderId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setString(3, item.getSize());
                    psItem.setInt(4, item.getQuantity());
                    psItem.setBigDecimal(5, item.getProductPrice());
                    psItem.executeUpdate();

                    int itemId = -1;
                    try (ResultSet rs = psItem.getGeneratedKeys()) {
                        if (rs.next()) itemId = rs.getInt(1);
                    }

                    createdItems.add(new OrderItem(
                            itemId,
                            orderId,
                            item.getProductId(),
                            item.getProductName(),
                            item.getProductImageUrl(),
                            item.getSize(),
                            item.getQuantity(),
                            item.getProductPrice()
                    ));
                }
            }

            // 3. Clear Cart
            String clearCartSql = "DELETE FROM cart_items WHERE user_id = ?";
            try (PreparedStatement psClear = conn.prepareStatement(clearCartSql)) {
                psClear.setInt(1, userId);
                psClear.executeUpdate();
            }

            conn.commit(); // Transaction Committed Successfully!

            Order order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
            order.setTotalAmount(totalAmount);
            order.setDiscount(discount);
            order.setStatus("Placed");
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(initialPaymentStatus);
            order.setCardLast4(cardLast4);
            order.setCardBrand(cardBrand);
            order.setAddress(address);
            order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            order.setItems(createdItems);
            return order;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[OrderDAO placeOrder ERROR]: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DatabaseUtil.releaseConnection(conn);
            }
        }
    }

    public List<Order> listByUser(int userId) {
        String sql = "SELECT id, user_id, total_amount, discount, status, payment_method, payment_status, " +
                "cancel_reason, cancelled_at, card_last4, card_brand, address, created_at " +
                "FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Order o = mapOrderRow(rs);
                        o.setItems(loadOrderItems(conn, o.getId()));
                        list.add(o);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO listByUser]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public List<Order> listAll() {
        String sql = "SELECT o.id, o.user_id, o.total_amount, o.discount, o.status, o.payment_method, o.payment_status, " +
                "o.cancel_reason, o.cancelled_at, o.card_last4, o.card_brand, o.address, o.created_at, " +
                "u.name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "LEFT JOIN users u ON o.user_id = u.id " +
                "ORDER BY o.created_at DESC";
        List<Order> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = mapOrderRow(rs);
                    o.setCustomerName(rs.getString("customer_name"));
                    o.setCustomerEmail(rs.getString("customer_email"));
                    o.setItems(loadOrderItems(conn, o.getId()));
                    list.add(o);
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO listAll]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return list;
    }

    public Order findById(int id) {
        String sql = "SELECT o.id, o.user_id, o.total_amount, o.discount, o.status, o.payment_method, o.payment_status, " +
                "o.cancel_reason, o.cancelled_at, o.card_last4, o.card_brand, o.address, o.created_at, " +
                "u.name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "LEFT JOIN users u ON o.user_id = u.id " +
                "WHERE o.id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Order o = mapOrderRow(rs);
                        o.setCustomerName(rs.getString("customer_name"));
                        o.setCustomerEmail(rs.getString("customer_email"));
                        o.setItems(loadOrderItems(conn, o.getId()));
                        return o;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO findById]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Cancels an order inside an atomic database transaction:
     * 1. Locks the order row with FOR UPDATE.
     * 2. Validates that current status is Placed or Packed.
     * 3. Restores stock for all order items.
     * 4. Updates status to Cancelled and payment_status to REFUND_PENDING (if online paid).
     * 5. Commits transaction and returns the updated order.
     */
    public Order cancelOrder(int orderId, String cancelReason) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Begin ACID transaction

            // 1. Lock order row
            String lockOrderSql = "SELECT id, user_id, status, payment_method, payment_status FROM orders WHERE id = ? FOR UPDATE";
            String currentStatus = null;
            String paymentMethod = null;
            String currentPaymentStatus = null;

            try (PreparedStatement ps = conn.prepareStatement(lockOrderSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Order #" + orderId + " not found.");
                    }
                    currentStatus = rs.getString("status");
                    paymentMethod = rs.getString("payment_method");
                    currentPaymentStatus = rs.getString("payment_status");
                }
            }

            // 2. Validate status allows cancellation
            if ("Cancelled".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                throw new IllegalStateException("Order #" + orderId + " is already cancelled.");
            }
            if (!"Placed".equalsIgnoreCase(currentStatus) && !"Packed".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                throw new IllegalStateException("Order #" + orderId + " cannot be cancelled as it is already " + currentStatus + ".");
            }

            // 3. Load order items and restore inventory stock
            String itemsSql = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
            String restoreStockSql = "UPDATE products SET stock = stock + ? WHERE id = ?";

            try (PreparedStatement psItems = conn.prepareStatement(itemsSql);
                 PreparedStatement psRestore = conn.prepareStatement(restoreStockSql)) {
                psItems.setInt(1, orderId);
                try (ResultSet rs = psItems.executeQuery()) {
                    while (rs.next()) {
                        int productId = rs.getInt("product_id");
                        int qty = rs.getInt("quantity");

                        psRestore.setInt(1, qty);
                        psRestore.setInt(2, productId);
                        psRestore.executeUpdate();
                    }
                }
            }

            // 4. Determine refund status
            String newPaymentStatus = currentPaymentStatus;
            boolean isOnlinePaid = "PAID".equalsIgnoreCase(currentPaymentStatus) || 
                                   ("CARD".equalsIgnoreCase(paymentMethod) || "UPI".equalsIgnoreCase(paymentMethod));
            if (isOnlinePaid && !"COD".equalsIgnoreCase(paymentMethod)) {
                newPaymentStatus = "REFUND_PENDING";
            }

            // 5. Update order record
            String updateSql = "UPDATE orders SET status = 'Cancelled', cancel_reason = ?, cancelled_at = NOW(), payment_status = ? WHERE id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, cancelReason != null && !cancelReason.isBlank() ? cancelReason.trim() : "Cancelled by user");
                psUpdate.setString(2, newPaymentStatus);
                psUpdate.setInt(3, orderId);
                psUpdate.executeUpdate();
            }

            conn.commit(); // Transaction committed successfully!

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DatabaseUtil.releaseConnection(conn);
            }
        }

        return findById(orderId);
    }

    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, orderId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO updateStatus]: " + e.getMessage());
            return false;
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
    }

    private List<OrderItem> loadOrderItems(Connection conn, int orderId) {
        String sql = "SELECT oi.id, oi.order_id, oi.product_id, oi.size, oi.quantity, oi.price, " +
                "p.name AS product_name, p.image_url " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("image_url"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO loadOrderItems]: " + e.getMessage());
        }
        return items;
    }

    public Map<String, Object> getAdminKPIs() {
        Map<String, Object> kpis = new HashMap<>();
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();

            // Total Revenue & Orders
            String revSql = "SELECT COUNT(*) as total_orders, COALESCE(SUM(total_amount), 0) as total_revenue FROM orders WHERE status != 'Cancelled'";
            try (PreparedStatement ps = conn.prepareStatement(revSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    kpis.put("totalOrders", rs.getInt("total_orders"));
                    kpis.put("totalRevenue", rs.getBigDecimal("total_revenue"));
                }
            }

            // Total Customers
            String custSql = "SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'";
            try (PreparedStatement ps = conn.prepareStatement(custSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    kpis.put("totalCustomers", rs.getInt(1));
                }
            }

            // Low Stock Count
            String stockSql = "SELECT COUNT(*) FROM products WHERE stock <= 15";
            try (PreparedStatement ps = conn.prepareStatement(stockSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    kpis.put("lowStockCount", rs.getInt(1));
                }
            }

            // Recent 7 Days Sales Trend (for pure Canvas/SVG chart, excluding Cancelled orders)
            String trendSql = "SELECT DATE_FORMAT(created_at, '%b %d') as order_date, COUNT(*) as orders_count, COALESCE(SUM(total_amount), 0) as daily_revenue " +
                    "FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND status != 'Cancelled' " +
                    "GROUP BY DATE(created_at), DATE_FORMAT(created_at, '%b %d') " +
                    "ORDER BY DATE(created_at) ASC";
            List<Map<String, Object>> trend = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(trendSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", rs.getString("order_date"));
                    day.put("orders", rs.getInt("orders_count"));
                    day.put("revenue", rs.getBigDecimal("daily_revenue"));
                    trend.add(day);
                }
            }
            kpis.put("salesTrend", trend);

        } catch (SQLException e) {
            System.err.println("[OrderDAO getAdminKPIs]: " + e.getMessage());
        } finally {
            DatabaseUtil.releaseConnection(conn);
        }
        return kpis;
    }

    private Order mapOrderRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setDiscount(rs.getBigDecimal("discount"));
        o.setStatus(rs.getString("status"));
        o.setPaymentMethod(rs.getString("payment_method"));
        try { o.setPaymentStatus(rs.getString("payment_status")); } catch (SQLException ignored) {}
        try { o.setCancelReason(rs.getString("cancel_reason")); } catch (SQLException ignored) {}
        try { o.setCancelledAt(rs.getTimestamp("cancelled_at")); } catch (SQLException ignored) {}
        try { o.setCardLast4(rs.getString("card_last4")); } catch (SQLException ignored) {}
        try { o.setCardBrand(rs.getString("card_brand")); } catch (SQLException ignored) {}
        o.setAddress(rs.getString("address"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        return o;
    }
}
